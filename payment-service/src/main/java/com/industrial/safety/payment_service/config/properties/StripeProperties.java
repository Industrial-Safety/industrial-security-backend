package com.industrial.safety.payment_service.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        @NotNull Api api,
        @NotNull Simulator simulator
) {
    public record Api(
            @NotBlank String baseUrl,
            @NotBlank String secretKey,
            @NotBlank String webhookSecret,
            int timeoutMillis
    ) {}

    public record Simulator(
            boolean enabled,
            @NotBlank String successCard,
            @NotBlank String declineCard
    ) {}
}
