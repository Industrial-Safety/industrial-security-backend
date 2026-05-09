package com.industrial.safety.payment_service.service;

import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.dto.stripe.StripeWebhookEvent;

public interface PaymentService {

    PaymentResponse processOrder(OrderCreatedEvent event);

    PaymentResponse getByOrderNumber(String orderNumber);

    void handleWebhook(StripeWebhookEvent event);
}
