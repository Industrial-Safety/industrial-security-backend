package com.industrial.safety.incidencias.integration;

import com.industrial.safety.incidencias.entity.Prioridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraClient — mapeo de prioridad")
class JiraClientTest {

    @Test
    @DisplayName("mapea la prioridad interna a la escala de Jira (Highest..Low)")
    void mapeaPrioridad() {
        assertThat(JiraClient.mapPrioridad(Prioridad.CRITICA)).isEqualTo("Highest");
        assertThat(JiraClient.mapPrioridad(Prioridad.ALTA)).isEqualTo("High");
        assertThat(JiraClient.mapPrioridad(Prioridad.MEDIA)).isEqualTo("Medium");
        assertThat(JiraClient.mapPrioridad(Prioridad.BAJA)).isEqualTo("Low");
        assertThat(JiraClient.mapPrioridad(null)).isEqualTo("Medium");
    }
}
