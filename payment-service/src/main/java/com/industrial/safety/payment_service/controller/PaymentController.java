package com.industrial.safety.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.payment_service.client.StripeClient;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.stripe.StripeWebhookEvent;
import com.industrial.safety.payment_service.exception.InvalidWebhookSignatureException;
import com.industrial.safety.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";
    private final PaymentService paymentService;
    private final StripeClient stripeClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/{orderNumber}")
    public PaymentResponse getByOrderNumber(@PathVariable String orderNumber) {
        return paymentService.getByOrderNumber(orderNumber);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = STRIPE_SIGNATURE_HEADER, required = false) String signature) throws IOException {
        if (!stripeClient.isValidSignature(rawBody, signature)) {
            throw new InvalidWebhookSignatureException("Stripe webhook signature mismatch");
        }
        StripeWebhookEvent event = objectMapper.readValue(rawBody, StripeWebhookEvent.class);
        log.info("Webhook received: id={} type={}", event.id(), event.type());
        paymentService.handleWebhook(event);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
