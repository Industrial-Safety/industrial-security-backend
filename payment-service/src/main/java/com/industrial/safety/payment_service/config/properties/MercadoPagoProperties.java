package com.industrial.safety.payment_service.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
        @NotNull Api api,
        Webhook webhook
) {
    public record Api(
            @NotBlank String baseUrl,
            @NotBlank String accessToken,
            int timeoutMillis
    ) {}

    public record Webhook(
            String secret
    ) {}
}
