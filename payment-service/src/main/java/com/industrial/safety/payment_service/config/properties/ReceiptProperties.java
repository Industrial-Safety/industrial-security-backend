package com.industrial.safety.payment_service.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "receipt")
public record ReceiptProperties(
        @NotNull Storage storage
) {
    public record Storage(
            @NotBlank String outputDir,
            @NotBlank String publicBaseUrl
    ) {}
}
