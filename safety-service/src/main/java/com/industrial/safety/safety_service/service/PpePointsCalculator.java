package com.industrial.safety.safety_service.service;

import com.industrial.safety.safety_service.config.SafetyPointsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

/**
 * Traduce las etiquetas de EPP faltante (que envía la IA) a puntos a descontar.
 * Responsabilidad única: el cálculo. No persiste ni publica nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PpePointsCalculator {

    private final SafetyPointsProperties properties;

    /** Suma de descuentos por cada EPP faltante en el incidente. */
    public int totalDeduction(List<String> violationTypes) {
        if (violationTypes == null || violationTypes.isEmpty()) {
            return 0;
        }
        return violationTypes.stream()
                .mapToInt(this::deductionFor)
                .sum();
    }

    private int deductionFor(String rawLabel) {
        String label = normalize(rawLabel);
        if (label.contains("casco")) {
            return properties.cascoOrDefault();
        }
        if (label.contains("guante")) {
            return properties.guantesOrDefault();
        }
        if (label.contains("chaleco") || label.contains("vestimenta")) {
            return properties.chalecoOrDefault();
        }
        log.warn("Etiqueta de EPP no reconocida, sin descuento: '{}'", rawLabel);
        return 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped.trim().toLowerCase();
    }
}
