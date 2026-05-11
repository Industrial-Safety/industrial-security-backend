package com.industrial.safety.order_service.service.impl;

import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.dto.event.EmailNotificationEvent;
import com.industrial.safety.order_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.order_service.dto.event.OrderItemEvent;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.dto.event.WebAlertEvent;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.mapper.OrderMapper;
import com.industrial.safety.order_service.messaging.OrderEventPublisher;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.repository.OrderRepository;
import com.industrial.safety.order_service.service.CouponService;
import com.industrial.safety.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        if (request.getOrderLineItemsList() == null || request.getOrderLineItemsList().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one course");
        }

        List<OrderLineItems> entities = request.getOrderLineItemsList().stream()
                .map(orderMapper::toEntity)
                .toList();

        BigDecimal rawTotal = entities.stream()
                .map(OrderLineItems::getPrice)
                .filter(p -> p != null && p.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (rawTotal.signum() <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }

        // Apply coupon if provided — validation happens here, consumption happens on payment confirm
        BigDecimal total = rawTotal;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            String firstCourseId = entities.isEmpty() ? null : entities.get(0).getIdCurso();
            Coupon coupon = couponService.validateAndGet(request.getCouponCode(), firstCourseId);
            discountAmount = coupon.discountAmount(rawTotal);
            total = coupon.applyTo(rawTotal);
            couponCode = coupon.getCode();
            log.info("Coupon {} applied to order: -{} -> final total {}", couponCode, discountAmount, total);
        }

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .currency(request.getCurrency() == null ? "USD" : request.getCurrency().toUpperCase())
                .originalAmount(rawTotal)
                .discountAmount(discountAmount)
                .totalAmount(total)
                .couponCode(couponCode)
                .orderStatus(OrderStatus.PENDING)
                .orderLineItemsList(entities)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order persisted as PENDING: {}", saved.getOrderNumber());

        orderEventPublisher.publishOrderCreated(toOrderCreatedEvent(saved, request));
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a COMPLETED order. Use refund flow instead.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateStatus(String orderNumber, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        if (order.getOrderStatus() == newStatus) {
            return;
        }
        if (!isLegalTransition(order.getOrderStatus(), newStatus)) {
            throw new IllegalStateException("Illegal transition: " + order.getOrderStatus() + " -> " + newStatus);
        }
        order.setOrderStatus(newStatus);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void processPaymentResult(PaymentResultEvent event) {
        Order order = orderRepository.findByOrderNumber(event.orderNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", event.orderNumber()));

        // Idempotency: terminal states are sticky.
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            log.info("Ignoring payment result for already COMPLETED order {}", event.orderNumber());
            return;
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.warn("Payment result arrived for CANCELLED order {} — recorded but no action", event.orderNumber());
            return;
        }

        if (event.success()) {
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setPaymentIntentId(event.paymentIntentId());
            order.setReceiptUrl(event.receiptUrl());
            order.setPaidAt(event.occurredAt() == null ? Instant.now() : event.occurredAt());
            order.setFailureReason(null);
            orderRepository.save(order);

            // Consume coupon use only after confirmed payment
            if (order.getCouponCode() != null) {
                couponService.consumeUse(order.getCouponCode());
            }

            fanOutSuccess(order, event);
        } else {
            order.setOrderStatus(OrderStatus.FAILED);
            order.setFailureReason(event.failureReason());
            order.setPaymentIntentId(event.paymentIntentId());
            orderRepository.save(order);
            fanOutFailure(order, event);
        }
    }

    private void fanOutSuccess(Order order, PaymentResultEvent event) {
        String courseSummary = event.items() == null ? buildSummary(order) :
                event.items().stream()
                        .map(i -> i.courseName() == null ? i.courseId() : i.courseName())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(", "));

        if (order.getUserEmail() != null && !order.getUserEmail().isBlank()) {
            orderEventPublisher.publishEmail(new EmailNotificationEvent(
                    order.getUserEmail(),
                    "Your purchase is confirmed: " + order.getOrderNumber(),
                    courseSummary,
                    event.receiptUrl()
            ), true);
        } else {
            log.warn("Order {} has no email — skipping email notification", order.getOrderNumber());
        }

        orderEventPublisher.publishWebAlert(new WebAlertEvent(
                order.getUserId(),
                "Course unlocked",
                "Your access to " + courseSummary + " is now available."
        ), true);
    }

    private void fanOutFailure(Order order, PaymentResultEvent event) {
        String courseSummary = buildSummary(order);
        if (order.getUserEmail() != null && !order.getUserEmail().isBlank()) {
            orderEventPublisher.publishEmail(new EmailNotificationEvent(
                    order.getUserEmail(),
                    "Payment failed for order " + order.getOrderNumber(),
                    courseSummary,
                    null
            ), false);
        }
        orderEventPublisher.publishWebAlert(new WebAlertEvent(
                order.getUserId(),
                "Payment failed",
                "We couldn't process your payment: "
                        + (event.failureReason() == null ? "please try again." : event.failureReason())
        ), false);
    }

    private OrderCreatedEvent toOrderCreatedEvent(Order order, OrderRequest request) {
        List<OrderItemEvent> items = order.getOrderLineItemsList() == null ? List.of() :
                order.getOrderLineItemsList().stream()
                        .map(li -> new OrderItemEvent(li.getIdCurso(), li.getCourseName(), li.getPrice()))
                        .toList();
        return new OrderCreatedEvent(
                order.getOrderNumber(),
                order.getUserId(),
                order.getUserEmail(),
                request.getMpToken(),
                request.getMpPaymentMethodId(),
                request.getMpInstallments(),
                request.getMpIssuerId(),
                request.getMpPayerEmail(),
                request.getMpPayerIdType(),
                request.getMpPayerIdNumber(),
                order.getCurrency(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt()
        );
    }

    private String buildSummary(Order order) {
        if (order.getOrderLineItemsList() == null || order.getOrderLineItemsList().isEmpty()) {
            return "Course purchase";
        }
        return order.getOrderLineItemsList().stream()
                .map(li -> li.getCourseName() == null ? li.getIdCurso() : li.getCourseName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private boolean isLegalTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.PROCESSING || to == OrderStatus.COMPLETED
                    || to == OrderStatus.FAILED || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.COMPLETED || to == OrderStatus.FAILED;
            case FAILED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
