package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.config.UmbralProperties;
import com.industrial.safety.eventos.entity.NivelEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PoliticaUmbrales — umbrales de deteccion (S15/S29 diapositiva 30)")
class PoliticaUmbralesTest {

    private final PoliticaUmbrales politica = new PoliticaUmbrales(new UmbralProperties());

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @CsvSource({
            "cpu,             60,   INFORMACION",
            "cpu,             72,   INFORMACION",
            "cpu,             75,   WARNING",
            "cpu,             80,   WARNING",
            "cpu,             85,   ERROR",
            "cpu,             90,   ERROR",
            "cpu,             95,   CRITICAL",
            "cpu,             100,  CRITICAL",
            "ram,             85,   ERROR",
            "disco,           95,   CRITICAL",
            "login_fallidos,  4,    INFORMACION",
            "login_fallidos,  25,   ERROR",
            "login_fallidos,  30,   CRITICAL",
            "bd_latencia_ms,  1800, ERROR"
    })
    void clasificaPorBanda(String metrica, double valor, NivelEvento esperado) {
        assertThat(politica.evaluar(metrica, valor)).contains(esperado);
    }

    @Test
    @DisplayName("acepta alias de metricas (CPUUtilization -> cpu)")
    void reconoceAlias() {
        assertThat(politica.tieneUmbral("CPUUtilization")).isTrue();
        assertThat(politica.evaluar("CPUUtilization", 96)).contains(NivelEvento.CRITICAL);
        assertThat(politica.evaluar("memory", 90)).contains(NivelEvento.ERROR);
    }

    @Test
    @DisplayName("metrica sin politica -> sin umbral")
    void metricaDesconocida() {
        assertThat(politica.tieneUmbral("temperatura")).isFalse();
        assertThat(politica.evaluar("temperatura", 50)).isEmpty();
    }

    @Test
    @DisplayName("la configuracion sobreescribe los defaults")
    void overridePorConfig() {
        UmbralProperties props = new UmbralProperties();
        props.setUmbrales(Map.of("cpu", List.of("0:INFORMACION", "50:CRITICAL")));
        PoliticaUmbrales custom = new PoliticaUmbrales(props);

        assertThat(custom.evaluar("cpu", 60)).contains(NivelEvento.CRITICAL);
        assertThat(custom.evaluar("cpu", 40)).contains(NivelEvento.INFORMACION);
    }
}
