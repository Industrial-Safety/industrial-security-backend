package com.industrial.safety.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        Email email,
        Websocket websocket
) {
    public record Email(
            String from,
            String fromName
    ) {}

    public record Websocket(
            String endpoint,
            String userDestinationPrefix,
            String brokerPrefix
    ) {}
}
