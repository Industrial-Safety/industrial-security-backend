package com.industrial.safety.payment_service.unit.client;

import com.industrial.safety.payment_service.client.MercadoPagoApiClient;
import com.industrial.safety.payment_service.config.properties.MercadoPagoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("MercadoPagoApiClient — Validación de firma de webhook")
class MercadoPagoApiClientTest {

    private MercadoPagoApiClient clientWithSecret(String secret) {
        MercadoPagoProperties props = new MercadoPagoProperties(
                new MercadoPagoProperties.Api("http://mp", "token", 5000),
                new MercadoPagoProperties.Webhook(secret));
        return new MercadoPagoApiClient(mock(WebClient.class), props);
    }

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(computed);
    }

    @Test
    @DisplayName("sin secreto configurado -> acepta (modo dev)")
    void noSecret_acceptsAll() {
        assertThat(clientWithSecret(null).isValidWebhookSignature("payload", "ts=1,v1=abc")).isTrue();
        assertThat(clientWithSecret("  ").isValidWebhookSignature("payload", "anything")).isTrue();
    }

    @Test
    @DisplayName("payload o firma null -> rechaza")
    void nullPayloadOrSignature_rejected() {
        MercadoPagoApiClient client = clientWithSecret("s3cr3t");
        assertThat(client.isValidWebhookSignature(null, "ts=1,v1=abc")).isFalse();
        assertThat(client.isValidWebhookSignature("payload", null)).isFalse();
    }

    @Test
    @DisplayName("firma v1 válida -> acepta")
    void validSignature_accepted() throws Exception {
        String payload = "id=123;request-id=abc";
        String secret = "s3cr3t";
        String v1 = sign(payload, secret);

        assertThat(clientWithSecret(secret).isValidWebhookSignature(payload, "ts=1700000000,v1=" + v1)).isTrue();
    }

    @Test
    @DisplayName("firma incorrecta -> rechaza")
    void invalidSignature_rejected() {
        assertThat(clientWithSecret("s3cr3t").isValidWebhookSignature("payload", "ts=1,v1=deadbeef")).isFalse();
    }

    @Test
    @DisplayName("header sin par v1 -> usa el header completo (firma inválida)")
    void headerWithoutV1_rejected() {
        assertThat(clientWithSecret("s3cr3t").isValidWebhookSignature("payload", "soloalgo")).isFalse();
    }
}
