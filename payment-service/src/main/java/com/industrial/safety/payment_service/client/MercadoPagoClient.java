package com.industrial.safety.payment_service.client;

import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentRequest;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;

public interface MercadoPagoClient {

    MercadoPagoPaymentResponse createPayment(MercadoPagoPaymentRequest request, String idempotencyKey);

    MercadoPagoPaymentResponse getPayment(String paymentId);

    boolean isValidWebhookSignature(String payload, String signatureHeader);
}
