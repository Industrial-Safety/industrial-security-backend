package com.industrial.safety.safety_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.safety.points")
public record SafetyPointsProperties(
        Integer base,
        Integer casco,
        Integer guantes,
        Integer chaleco
) {
    public int baseOrDefault() {
        return base != null ? base : 100;
    }

    public int cascoOrDefault() {
        return casco != null ? casco : 20;
    }

    public int guantesOrDefault() {
        return guantes != null ? guantes : 5;
    }

    public int chalecoOrDefault() {
        return chaleco != null ? chaleco : 10;
    }
}
