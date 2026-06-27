package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.entity.Prioridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PrioridadCalculator — matriz Impacto x Urgencia")
class PrioridadCalculatorTest {

    @ParameterizedTest(name = "impacto={0}, urgencia={1} -> {2}")
    @CsvSource({
            "ALTO,  ALTO,  CRITICA",
            "ALTO,  MEDIO, ALTA",
            "ALTO,  BAJO,  MEDIA",
            "MEDIO, ALTO,  ALTA",
            "MEDIO, MEDIO, MEDIA",
            "MEDIO, BAJO,  BAJA",
            "BAJO,  ALTO,  MEDIA",
            "BAJO,  MEDIO, BAJA",
            "BAJO,  BAJO,  BAJA"
    })
    void calculaLaPrioridadSegunLaMatriz(Nivel impacto, Nivel urgencia, Prioridad esperada) {
        assertThat(PrioridadCalculator.calcular(impacto, urgencia)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("lanza excepción si falta impacto o urgencia")
    void lanzaSiFaltaAlgunNivel() {
        assertThatThrownBy(() -> PrioridadCalculator.calcular(null, Nivel.ALTO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PrioridadCalculator.calcular(Nivel.ALTO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
