package com.industrial.safety.payment_service.repository;

import com.industrial.safety.payment_service.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderNumber(String orderNumber);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    boolean existsByOrderNumber(String orderNumber);
}
