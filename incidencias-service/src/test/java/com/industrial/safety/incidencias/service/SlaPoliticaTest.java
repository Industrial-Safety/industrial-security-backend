package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Prioridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SlaPolitica — RTO de atención por prioridad (S16/S31)")
class SlaPoliticaTest {

    @ParameterizedTest(name = "{0} -> {1} minutos")
    @CsvSource({
            "CRITICA, 60",
            "ALTA,    120",
            "MEDIA,   240",
            "BAJA,    480"
    })
    void asignaMinutosSegunPrioridad(Prioridad prioridad, int esperado) {
        assertThat(SlaPolitica.minutos(prioridad)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("lanza excepción si falta la prioridad")
    void lanzaSinPrioridad() {
        assertThatThrownBy(() -> SlaPolitica.minutos(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
