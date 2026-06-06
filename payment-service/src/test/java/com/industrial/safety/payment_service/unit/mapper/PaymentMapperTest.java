package com.industrial.safety.payment_service.unit.mapper;

import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.mapper.PaymentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMapper — Pruebas Unitarias")
class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    @DisplayName("toResponse: mapea todos los campos del pago")
    void toResponse_mapsAllFields() {
        Payment payment = Payment.builder()
                .id(1L).orderNumber("ORD-1").paymentIntentId("pi-1")
                .amount(new BigDecimal("99.99")).currency("USD")
                .status(PaymentStatus.SUCCEEDED).failureReason(null)
                .receiptUrl("s3://r.pdf").createdAt(Instant.now()).paidAt(Instant.now())
                .build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.orderNumber()).isEqualTo("ORD-1");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.amount()).isEqualByComparingTo("99.99");
    }
}
