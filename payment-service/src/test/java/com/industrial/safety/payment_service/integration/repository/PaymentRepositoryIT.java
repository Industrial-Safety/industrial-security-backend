package com.industrial.safety.payment_service.integration.repository;

import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.industrial.safety.payment_service.integration.BasePaymentIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
@TestPropertySource(properties = {"spring.config.import=", "spring.cloud.aws.parameterstore.enabled=false"})
@DisplayName("PaymentRepository — Pruebas de Integración con PostgreSQL")
class PaymentRepositoryIT extends BasePaymentIT {

    @Autowired
    PaymentRepository paymentRepository;

    private Payment payment1;
    private Payment payment2;

    @BeforeEach
    void setUp() {
        payment1 = paymentRepository.save(
                Payment.builder()
                        .orderNumber("ORD-001")
                        .userId("user-A")
                        .userEmail("user-a@example.com")
                        .amount(new BigDecimal("39.99"))
                        .currency("USD")
                        .paymentIntentId("pi_001")
                        .idempotencyKey("idem-key-001")
                        .status(PaymentStatus.SUCCEEDED)
                        .build()
        );

        payment2 = paymentRepository.save(
                Payment.builder()
                        .orderNumber("ORD-002")
                        .userId("user-B")
                        .userEmail("user-b@example.com")
                        .amount(new BigDecimal("59.99"))
                        .currency("USD")
                        .paymentIntentId("pi_002")
                        .idempotencyKey("idem-key-002")
                        .status(PaymentStatus.PENDING)
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
    }

    // =========================================================
    //  findByOrderNumber
    // =========================================================

    @Test
    @DisplayName("findByOrderNumber: devuelve el pago cuando el número de orden existe")
    void findByOrderNumber_found() {
        Optional<Payment> result = paymentRepository.findByOrderNumber("ORD-001");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.get().getAmount()).isEqualByComparingTo("39.99");
    }

    @Test
    @DisplayName("findByOrderNumber: devuelve empty cuando el número no existe")
    void findByOrderNumber_notFound() {
        assertThat(paymentRepository.findByOrderNumber("ORD-FALSO")).isEmpty();
    }

    // =========================================================
    //  findByPaymentIntentId
    // =========================================================

    @Test
    @DisplayName("findByPaymentIntentId: devuelve el pago cuando el intent existe")
    void findByPaymentIntentId_found() {
        Optional<Payment> result = paymentRepository.findByPaymentIntentId("pi_002");

        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("findByPaymentIntentId: devuelve empty cuando no existe")
    void findByPaymentIntentId_notFound() {
        assertThat(paymentRepository.findByPaymentIntentId("pi_inexistente")).isEmpty();
    }

    // =========================================================
    //  existsByOrderNumber
    // =========================================================

    @Test
    @DisplayName("existsByOrderNumber: true cuando el número existe")
    void existsByOrderNumber_true() {
        assertThat(paymentRepository.existsByOrderNumber("ORD-001")).isTrue();
    }

    @Test
    @DisplayName("existsByOrderNumber: false cuando el número no existe")
    void existsByOrderNumber_false() {
        assertThat(paymentRepository.existsByOrderNumber("ORD-NO-EXISTE")).isFalse();
    }

    // =========================================================
    //  save / update
    // =========================================================

    @Test
    @DisplayName("save: actualiza el estado del pago correctamente")
    void save_updatesPaymentStatus() {
        payment2.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment2);

        Optional<Payment> updated = paymentRepository.findByOrderNumber("ORD-002");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }
}
