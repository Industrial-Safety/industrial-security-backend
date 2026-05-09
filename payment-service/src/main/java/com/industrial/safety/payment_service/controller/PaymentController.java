package com.industrial.safety.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.payment_service.client.MercadoPagoClient;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoWebhookEvent;
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

    private static final String MP_SIGNATURE_HEADER = "x-signature";

    private final PaymentService paymentService;
    private final MercadoPagoClient mercadoPagoClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/{orderNumber}")
    public PaymentResponse getByOrderNumber(@PathVariable String orderNumber) {
        return paymentService.getByOrderNumber(orderNumber);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = MP_SIGNATURE_HEADER, required = false) String signature) throws IOException {
        if (!mercadoPagoClient.isValidWebhookSignature(rawBody, signature)) {
            throw new InvalidWebhookSignatureException("MercadoPago webhook signature mismatch");
        }
        MercadoPagoWebhookEvent event = objectMapper.readValue(rawBody, MercadoPagoWebhookEvent.class);
        log.info("Webhook received: id={} type={} action={}", event.id(), event.type(), event.action());
        paymentService.handleWebhook(event);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
