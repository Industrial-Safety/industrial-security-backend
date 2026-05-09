package com.industrial.safety.payment_service.service.impl;

import com.industrial.safety.payment_service.client.StripeClient;
import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.dto.event.OrderItemEvent;
import com.industrial.safety.payment_service.dto.event.PaymentResultEvent;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentRequest;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentResponse;
import com.industrial.safety.payment_service.dto.stripe.StripeWebhookEvent;
import com.industrial.safety.payment_service.exception.PaymentNotFoundException;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.mapper.PaymentMapper;
import com.industrial.safety.payment_service.messaging.PaymentEventPublisher;
import com.industrial.safety.payment_service.pdf.ReceiptPdfGenerator;
import com.industrial.safety.payment_service.repository.PaymentRepository;
import com.industrial.safety.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final StripeClient stripeClient;
    private final ReceiptPdfGenerator receiptPdfGenerator;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public PaymentResponse processOrder(OrderCreatedEvent event) {
        validateEvent(event);

        Optional<Payment> existing = paymentRepository.findByOrderNumber(event.orderNumber());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                log.info("Idempotent replay: order {} already SUCCEEDED, re-emitting result event",
                        event.orderNumber());
                paymentEventPublisher.publishResult(toResultEvent(payment, event.items(), true, null));
                return paymentMapper.toResponse(payment);
            }
            if (payment.getStatus() == PaymentStatus.PROCESSING) {
                log.warn("Order {} is already being processed; skipping duplicate dispatch", event.orderNumber());
                return paymentMapper.toResponse(payment);
            }
            // For FAILED/CANCELLED we allow retry: reuse the row, advance state.
            return runChargeFlow(payment, event);
        }

        Payment payment = persistPending(event);
        return runChargeFlow(payment, event);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void handleWebhook(StripeWebhookEvent event) {
        if (event == null || event.type() == null) {
            log.warn("Ignoring invalid webhook event");
            return;
        }
        String paymentIntentId = extractPaymentIntentId(event);
        if (paymentIntentId == null) {
            log.warn("Webhook {} has no payment_intent id; ignoring", event.id());
            return;
        }
        Optional<Payment> maybe = paymentRepository.findByPaymentIntentId(paymentIntentId);
        if (maybe.isEmpty()) {
            log.warn("Webhook references unknown PaymentIntent {}", paymentIntentId);
            return;
        }
        Payment payment = maybe.get();
        switch (event.type()) {
            case StripeWebhookEvent.TYPE_PAYMENT_SUCCEEDED -> reconcileSucceeded(payment);
            case StripeWebhookEvent.TYPE_PAYMENT_FAILED -> reconcileFailed(payment, event);
            default -> log.debug("Unhandled webhook type {} for {}", event.type(), paymentIntentId);
        }
    }

    private Payment persistPending(OrderCreatedEvent event) {
        Payment payment = Payment.builder()
                .orderNumber(event.orderNumber())
                .userId(event.userId())
                .userEmail(event.userEmail())
                .courseSummary(buildCourseSummary(event.items()))
                .amount(event.totalAmount())
                .currency(safeCurrency(event.currency()))
                .idempotencyKey(UUID.randomUUID().toString())
                .status(PaymentStatus.PENDING)
                .build();
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert by a parallel consumer; reload the winning row.
            log.info("Concurrent insert detected for order {}; reloading", event.orderNumber());
            return paymentRepository.findByOrderNumber(event.orderNumber())
                    .orElseThrow(() -> ex);
        }
    }

    private PaymentResponse runChargeFlow(Payment payment, OrderCreatedEvent event) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.saveAndFlush(payment);

        StripePaymentIntentRequest request = new StripePaymentIntentRequest(
                payment.getOrderNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                event.paymentMethodToken(),
                payment.getUserEmail(),
                "Industrial Safety Tech - " + payment.getOrderNumber(),
                payment.getIdempotencyKey()
        );

        try {
            StripePaymentIntentResponse response = stripeClient.createAndConfirmPaymentIntent(request);
            payment.setPaymentIntentId(response.id());
            if (response.isSucceeded()) {
                return finalizeSuccess(payment, event.items());
            }
            return finalizeFailure(payment, event.items(),
                    response.failureCode() == null ? "card_declined" : response.failureCode(),
                    response.failureMessage() == null ? "Payment was not completed" : response.failureMessage());
        } catch (PaymentProcessingException ex) {
            return finalizeFailure(payment, event.items(), ex.getCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error processing payment for {}", payment.getOrderNumber(), ex);
            return finalizeFailure(payment, event.items(), "internal_error", "Unexpected error");
        }
    }

    private PaymentResponse finalizeSuccess(Payment payment, List<OrderItemEvent> items) {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());
        payment.setFailureReason(null);
        receiptPdfGenerator.generate(payment, items);
        payment.setReceiptUrl(receiptPdfGenerator.buildPublicUrl(payment.getOrderNumber()));
        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishResult(toResultEvent(saved, items, true, null));
        return paymentMapper.toResponse(saved);
    }

    private PaymentResponse finalizeFailure(Payment payment, List<OrderItemEvent> items,
                                            String code, String message) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(code + ": " + message);
        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishResult(toResultEvent(saved, items, false, payment.getFailureReason()));
        return paymentMapper.toResponse(saved);
    }

    private void reconcileSucceeded(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());
        receiptPdfGenerator.generate(payment, List.of());
        payment.setReceiptUrl(receiptPdfGenerator.buildPublicUrl(payment.getOrderNumber()));
        paymentRepository.save(payment);
        paymentEventPublisher.publishResult(toResultEvent(payment, List.of(), true, null));
    }

    private void reconcileFailed(Payment payment, StripeWebhookEvent event) {
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Webhook reports failure for already-succeeded payment {}; ignoring",
                    payment.getOrderNumber());
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Async failure from Stripe webhook " + event.id());
        paymentRepository.save(payment);
        paymentEventPublisher.publishResult(toResultEvent(payment, List.of(), false, payment.getFailureReason()));
    }

    private PaymentResultEvent toResultEvent(Payment payment, List<OrderItemEvent> items,
                                             boolean success, String failureReason) {
        return new PaymentResultEvent(
                payment.getOrderNumber(),
                payment.getPaymentIntentId(),
                payment.getUserId(),
                payment.getUserEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                success,
                failureReason,
                payment.getReceiptUrl(),
                items == null ? List.of() : items,
                Instant.now()
        );
    }

    private void validateEvent(OrderCreatedEvent event) {
        if (event == null || event.orderNumber() == null || event.orderNumber().isBlank()) {
            throw new PaymentProcessingException("invalid_event", "OrderCreatedEvent is missing orderNumber");
        }
        if (event.totalAmount() == null || event.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentProcessingException("invalid_amount", "totalAmount must be > 0");
        }
    }

    private String safeCurrency(String currency) {
        return (currency == null || currency.isBlank()) ? "USD" : currency.toUpperCase();
    }

    private String buildCourseSummary(List<OrderItemEvent> items) {
        if (items == null || items.isEmpty()) {
            return "Course purchase";
        }
        return items.stream()
                .map(i -> i.courseName() == null ? i.courseId() : i.courseName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private String extractPaymentIntentId(StripeWebhookEvent event) {
        if (event.data() == null || event.data().object() == null) {
            return null;
        }
        Object id = event.data().object().get("id");
        return id == null ? null : id.toString();
    }
}
