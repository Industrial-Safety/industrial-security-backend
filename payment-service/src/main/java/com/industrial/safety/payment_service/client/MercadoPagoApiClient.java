package com.industrial.safety.payment_service.client;

import com.industrial.safety.payment_service.config.properties.MercadoPagoProperties;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentRequest;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Component
public class MercadoPagoApiClient implements MercadoPagoClient {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final WebClient webClient;
    private final MercadoPagoProperties props;

    public MercadoPagoApiClient(@Qualifier("mercadoPagoWebClient") WebClient webClient,
                                MercadoPagoProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    @CircuitBreaker(name = "mercadopago", fallbackMethod = "fallbackCreatePayment")
    @Retry(name = "mercadopago")
    public MercadoPagoPaymentResponse createPayment(MercadoPagoPaymentRequest request,
                                                    String idempotencyKey) {
        try {
            return webClient.post()
                    .uri("/v1/payments")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MercadoPagoPaymentResponse.class)
                    .block(Duration.ofMillis(props.api().timeoutMillis()));
        } catch (WebClientResponseException ex) {
            log.warn("MercadoPago rejected payment for order {}: {}",
                    request.externalReference(), ex.getResponseBodyAsString());
            throw new PaymentProcessingException("mercadopago_error",
                    "MercadoPago rejected payment: " + ex.getStatusCode(), ex);
        }
    }

    @Override
    public MercadoPagoPaymentResponse getPayment(String paymentId) {
        try {
            return webClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .retrieve()
                    .bodyToMono(MercadoPagoPaymentResponse.class)
                    .block(Duration.ofMillis(props.api().timeoutMillis()));
        } catch (WebClientResponseException ex) {
            log.warn("MercadoPago getPayment failed for {}: {}", paymentId, ex.getResponseBodyAsString());
            throw new PaymentProcessingException("mercadopago_error",
                    "MercadoPago getPayment failed: " + ex.getStatusCode(), ex);
        }
    }

    @SuppressWarnings("unused")
    private MercadoPagoPaymentResponse fallbackCreatePayment(MercadoPagoPaymentRequest request,
                                                             String idempotencyKey,
                                                             Throwable ex) {
        log.error("MercadoPago circuit open for order {} — failing safely",
                request.externalReference(), ex);
        throw new PaymentProcessingException("mercadopago_unavailable",
                "Payment provider is temporarily unavailable. Please retry.", ex);
    }

    @Override
    public boolean isValidWebhookSignature(String payload, String signatureHeader) {
        String secret = props.webhook() == null ? null : props.webhook().secret();
        if (secret == null || secret.isBlank()) {
            // No secret configured — accept the webhook (dev mode).
            return true;
        }
        if (payload == null || signatureHeader == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    extractV1Signature(signatureHeader).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
            log.error("Failed to validate MercadoPago webhook signature", ex);
            return false;
        }
    }

    private String extractV1Signature(String header) {
        for (String pair : header.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "v1".equalsIgnoreCase(kv[0].trim())) {
                return kv[1].trim();
            }
        }
        return header.trim();
    }
}
