package com.industrial.safety.payment_service.unit.pdf;

import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.dto.event.OrderItemEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.pdf.ReceiptPdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReceiptPdfGenerator — Pruebas Unitarias")
class ReceiptPdfGeneratorTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock PresignedGetObjectRequest presigned;
    @InjectMocks ReceiptPdfGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(generator, "bucketName", "test-bucket");
        given(presigned.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/receipts/ORD-1.pdf?sig=x"));
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);
    }

    private Payment fullPayment() {
        return Payment.builder()
                .id(1L).orderNumber("ORD-1").userId("u1").userEmail("u@e.com")
                .amount(new BigDecimal("99.99")).currency("USD")
                .paymentIntentId("pi-1").status(PaymentStatus.SUCCEEDED).paidAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("generateAndUpload: genera PDF, sube a S3 y devuelve URL presignada")
    void generateAndUpload_full() {
        List<OrderItemEvent> items = List.of(
                new OrderItemEvent("c1", "Curso 1", new BigDecimal("49.99")),
                new OrderItemEvent("c2", null, null));

        String url = generator.generateAndUpload(fullPayment(), items);

        assertThat(url).startsWith("https://test-bucket.s3.amazonaws.com/receipts/");
        then(s3Client).should().putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("generateAndUpload: campos opcionales null e items null")
    void generateAndUpload_minimal() {
        Payment payment = Payment.builder()
                .orderNumber("ORD-2").amount(new BigDecimal("10.00")).currency("USD")
                .paymentIntentId(null).userEmail(null).paidAt(null).build();

        String url = generator.generateAndUpload(payment, null);

        assertThat(url).isNotBlank();
    }

    @Test
    @DisplayName("generateAndUpload: fallo al subir a S3 -> PaymentProcessingException")
    void generateAndUpload_s3Failure_throws() {
        willThrow(new RuntimeException("s3 down"))
                .given(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> generator.generateAndUpload(fullPayment(), List.of()))
                .isInstanceOf(PaymentProcessingException.class);
    }
}
