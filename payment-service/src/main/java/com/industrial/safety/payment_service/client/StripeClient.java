package com.industrial.safety.payment_service.client;

import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentRequest;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentResponse;

public interface StripeClient {

    StripePaymentIntentResponse createAndConfirmPaymentIntent(StripePaymentIntentRequest request);

    boolean isValidSignature(String payload, String signatureHeader);
}
