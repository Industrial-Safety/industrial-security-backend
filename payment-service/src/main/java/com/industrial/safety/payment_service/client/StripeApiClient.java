package com.industrial.safety.payment_service.client;

import com.industrial.safety.payment_service.config.properties.StripeProperties;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentRequest;
import com.industrial.safety.payment_service.dto.stripe.StripePaymentIntentResponse;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Production-grade Stripe client. Activated when stripe.simulator.enabled=false.
 * Wraps WebClient calls with Resilience4j circuit-breaker and retry policies.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "stripe.simulator", name = "enabled", havingValue = "false")
public class StripeApiClient implements StripeClient {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final WebClient webClient;
    private final StripeProperties props;

    public StripeApiClient(@Qualifier("stripeWebClient") WebClient webClient, StripeProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    @CircuitBreaker(name = "stripe", fallbackMethod = "fallbackPaymentIntent")
    @Retry(name = "stripe")
    public StripePaymentIntentResponse createAndConfirmPaymentIntent(StripePaymentIntentRequest request) {
        long amountCents = request.amount().setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", String.valueOf(amountCents));
        form.add("currency", request.currency().toLowerCase());
        form.add("payment_method", request.paymentMethodToken());
        form.add("confirm", "true");
        form.add("description", request.description());
        form.add("metadata[order_number]", request.orderNumber());
        if (request.receiptEmail() != null) {
            form.add("receipt_email", request.receiptEmail());
        }

        try {
            return webClient.post()
                    .uri("/v1/payment_intents")
                    .header("Idempotency-Key", request.idempotencyKey())
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(StripePaymentIntentResponse.class)
                    .block(Duration.ofMillis(props.api().timeoutMillis()));
        } catch (WebClientResponseException ex) {
            log.warn("Stripe API rejected payment {}: {}", request.orderNumber(), ex.getResponseBodyAsString());
            throw new PaymentProcessingException("stripe_error",
                    "Stripe rejected payment: " + ex.getStatusCode(), ex);
        }
    }

    @SuppressWarnings("unused")
    private StripePaymentIntentResponse fallbackPaymentIntent(StripePaymentIntentRequest request, Throwable ex) {
        log.error("Stripe circuit open for order {} — failing safely", request.orderNumber(), ex);
        throw new PaymentProcessingException("stripe_unavailable",
                "Payment provider is temporarily unavailable. Please retry.", ex);
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
                    extractSignature(signatureHeader).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
            log.error("Failed to validate Stripe webhook signature", ex);
            return false;
        }
    }

    private String extractSignature(String header) {
        for (String pair : header.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "v1".equals(kv[0].trim())) {
                return kv[1].trim();
            }
        }
        return header.trim();
    }
}
