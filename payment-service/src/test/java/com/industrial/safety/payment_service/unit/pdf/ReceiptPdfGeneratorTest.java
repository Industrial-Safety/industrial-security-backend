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
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptPdfGenerator — Pruebas Unitarias")
class ReceiptPdfGeneratorTest {

    @Mock S3Client    s3Client;
    @Mock S3Presigner s3Presigner;
    @InjectMocks ReceiptPdfGenerator generator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generator, "bucketName", "test-bucket");
    }

    private void stubPresigner() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://s3.example/receipts/r.pdf").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);
    }

    private Payment payment(boolean withFields) {
        return Payment.builder()
                .orderNumber("ORD-1")
                .paymentIntentId(withFields ? "mp-123" : null)
                .userEmail(withFields ? "user@example.com" : null)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paidAt(withFields ? Instant.now() : null)
                .status(PaymentStatus.SUCCEEDED)
                .build();
    }

    @Test
    @DisplayName("generateAndUpload: campos completos + items mixtos → sube y devuelve URL prefirmada")
    void generateAndUpload_happyPath() throws Exception {
        stubPresigner();
        List<OrderItemEvent> items = List.of(
                new OrderItemEvent("c1", "Curso A", new BigDecimal("49.99")),
                new OrderItemEvent(null, null, null));

        String url = generator.generateAndUpload(payment(true), items);

        assertThat(url).isEqualTo("https://s3.example/receipts/r.pdf");
    }

    @Test
    @DisplayName("generateAndUpload: campos null + items null → igual genera el PDF")
    void generateAndUpload_nullFields() throws Exception {
        stubPresigner();

        String url = generator.generateAndUpload(payment(false), null);

        assertThat(url).isEqualTo("https://s3.example/receipts/r.pdf");
    }

    @Test
    @DisplayName("generateAndUpload: fallo al subir a S3 → PaymentProcessingException")
    void generateAndUpload_s3Error() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> generator.generateAndUpload(payment(true), List.of()))
                .isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    @DisplayName("fallbackGenerateAndUpload → PaymentProcessingException (receipt_s3_unavailable)")
    void fallback_throws() throws Exception {
        Method m = ReceiptPdfGenerator.class.getDeclaredMethod(
                "fallbackGenerateAndUpload", Payment.class, List.class, Throwable.class);
        m.setAccessible(true);
        assertThatThrownBy(() -> {
            try {
                m.invoke(generator, payment(true), List.of(), new RuntimeException("cb open"));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(PaymentProcessingException.class);
    }
}
