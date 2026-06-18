package com.industrial.safety.payment_service.unit.client;

import com.industrial.safety.payment_service.client.MercadoPagoApiClient;
import com.industrial.safety.payment_service.config.properties.MercadoPagoProperties;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentRequest;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MercadoPagoApiClient — Pruebas Unitarias")
class MercadoPagoApiClientTest {

    private MercadoPagoProperties props(String secret) {
        return new MercadoPagoProperties(
                new MercadoPagoProperties.Api("http://mp", "token", 2000),
                new MercadoPagoProperties.Webhook(secret));
    }

    private MercadoPagoApiClient client(ClientResponse response, MercadoPagoProperties props) {
        ExchangeFunction ef = req -> Mono.just(response);
        WebClient wc = WebClient.builder().baseUrl("http://mp").exchangeFunction(ef).build();
        return new MercadoPagoApiClient(wc, props);
    }

    private MercadoPagoApiClient noHttpClient(MercadoPagoProperties props) {
        WebClient wc = WebClient.builder().baseUrl("http://mp")
                .exchangeFunction(req -> Mono.empty()).build();
        return new MercadoPagoApiClient(wc, props);
    }

    private MercadoPagoPaymentRequest request() {
        return new MercadoPagoPaymentRequest(
                new BigDecimal("99.99"), "tok", "desc", 1, "visa", null,
                new MercadoPagoPaymentRequest.Payer("e@x.com", null),
                Map.of(), "ORD-1", null, "DESC");
    }

    private ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    // ── createPayment ─────────────────────────────────────────────────────
    @Test
    @DisplayName("createPayment: 200 → parsea la respuesta aprobada")
    void createPayment_ok() {
        var c = client(json(HttpStatus.OK, "{\"id\":123,\"status\":\"approved\"}"), props("s"));
        MercadoPagoPaymentResponse r = c.createPayment(request(), "idem-1");
        assertThat(r.id()).isEqualTo(123L);
        assertThat(r.isApproved()).isTrue();
    }

    @Test
    @DisplayName("createPayment: error HTTP → PaymentProcessingException")
    void createPayment_httpError() {
        var c = client(json(HttpStatus.BAD_REQUEST, "{\"message\":\"bad\"}"), props("s"));
        assertThatThrownBy(() -> c.createPayment(request(), "idem-1"))
                .isInstanceOf(PaymentProcessingException.class);
    }

    // ── getPayment ────────────────────────────────────────────────────────
    @Test
    @DisplayName("getPayment: 200 → parsea la respuesta")
    void getPayment_ok() {
        var c = client(json(HttpStatus.OK, "{\"id\":777,\"status\":\"rejected\"}"), props("s"));
        MercadoPagoPaymentResponse r = c.getPayment("777");
        assertThat(r.id()).isEqualTo(777L);
        assertThat(r.isFailed()).isTrue();
    }

    @Test
    @DisplayName("getPayment: error HTTP → PaymentProcessingException")
    void getPayment_httpError() {
        var c = client(json(HttpStatus.INTERNAL_SERVER_ERROR, "boom"), props("s"));
        assertThatThrownBy(() -> c.getPayment("777"))
                .isInstanceOf(PaymentProcessingException.class);
    }

    // ── isValidWebhookSignature ───────────────────────────────────────────
    @Test
    @DisplayName("firma: sin secreto configurado → acepta (modo dev)")
    void signature_noSecret_accepts() {
        assertThat(noHttpClient(props(null)).isValidWebhookSignature("p", "v1=x")).isTrue();
    }

    @Test
    @DisplayName("firma: payload null → rechaza")
    void signature_nullPayload_rejects() {
        assertThat(noHttpClient(props("secret")).isValidWebhookSignature(null, "v1=x")).isFalse();
    }

    @Test
    @DisplayName("firma: HMAC válido (header con v1=) → acepta")
    void signature_validHmac_accepts() throws Exception {
        String secret = "topsecret";
        String payload = "hello-world";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hex = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        assertThat(noHttpClient(props(secret))
                .isValidWebhookSignature(payload, "ts=123,v1=" + hex)).isTrue();
    }

    @Test
    @DisplayName("firma: HMAC inválido → rechaza")
    void signature_invalidHmac_rejects() {
        assertThat(noHttpClient(props("topsecret"))
                .isValidWebhookSignature("hello", "ts=1,v1=deadbeef")).isFalse();
    }

    @Test
    @DisplayName("firma: header sin 'v1=' → usa el header completo (rechaza)")
    void signature_headerWithoutV1_rejects() {
        assertThat(noHttpClient(props("topsecret"))
                .isValidWebhookSignature("hello", "ts=1")).isFalse();
    }

    // ── fallback del circuit breaker ──────────────────────────────────────
    @Test
    @DisplayName("fallbackCreatePayment → PaymentProcessingException (mercadopago_unavailable)")
    void fallback_throws() throws Exception {
        var c = noHttpClient(props("s"));
        Method m = MercadoPagoApiClient.class.getDeclaredMethod(
                "fallbackCreatePayment", MercadoPagoPaymentRequest.class, String.class, Throwable.class);
        m.setAccessible(true);
        assertThatThrownBy(() -> {
            try {
                m.invoke(c, request(), "k", new RuntimeException("cb open"));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(PaymentProcessingException.class);
    }
}
