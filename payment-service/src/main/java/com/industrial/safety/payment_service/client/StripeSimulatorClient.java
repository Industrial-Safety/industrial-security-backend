package com.industrial.safety.payment_service.client;

import com.industrial.safety.payment_service.config.properties.StripeProperties;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentRequest;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentResponse;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * In-process simulator that mimics Stripe's PaymentIntents API without network calls.
 * Switch off via stripe.simulator.enabled=false to use the real WebClient-backed implementation.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "stripe.simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StripeSimulatorClient implements StripeClient {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final StripeProperties props;

    @Override
    public StripePaymentIntentResponse createAndConfirmPaymentIntent(StripePaymentIntentRequest request) {
        validateRequest(request);
        String token = request.paymentMethodToken() == null ? "" : request.paymentMethodToken().trim();
        long amountCents = request.amount().setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact();

        if (token.contains(props.simulator().declineCard())) {
            log.info("[stripe-sim] Declining payment for orderNumber={}", request.orderNumber());
            return new StripePaymentIntentResponse(
                    "pi_sim_" + UUID.randomUUID(),
                    StripePaymentIntentResponse.STATUS_REQUIRES_PAYMENT_METHOD,
                    null,
                    amountCents,
                    request.currency().toLowerCase(),
                    "card_declined",
                    "Your card was declined."
            );
        }

        // Default success path: any token containing the success card (4242...) succeeds.
        // We accept tokens not matching either to keep the demo permissive.
        log.info("[stripe-sim] Approving payment for orderNumber={} amount={} {}",
                request.orderNumber(), request.amount(), request.currency());
        return new StripePaymentIntentResponse(
                "pi_sim_" + UUID.randomUUID(),
                StripePaymentIntentResponse.STATUS_SUCCEEDED,
                "cs_sim_" + UUID.randomUUID(),
                amountCents,
                request.currency().toLowerCase(),
                null,
                null
        );
    }

    @Override
    public boolean isValidSignature(String payload, String signatureHeader) {
        if (payload == null || signatureHeader == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    props.api().webhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
            log.error("Failed to validate Stripe webhook signature", ex);
            return false;
        }
    }

    private void validateRequest(StripePaymentIntentRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new PaymentProcessingException("invalid_amount", "Amount must be positive");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new PaymentProcessingException("invalid_currency", "Currency is required");
        }
        if (request.orderNumber() == null || request.orderNumber().isBlank()) {
            throw new PaymentProcessingException("invalid_order", "Order number is required");
        }
    }
}
