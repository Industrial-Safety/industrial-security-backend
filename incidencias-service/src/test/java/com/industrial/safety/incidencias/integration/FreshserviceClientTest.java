package com.industrial.safety.incidencias.integration;

import com.industrial.safety.incidencias.entity.Prioridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FreshserviceClient — mapeo de prioridad")
class FreshserviceClientTest {

    @Test
    @DisplayName("mapea la prioridad interna a la escala de Freshservice (1..4)")
    void mapeaPrioridad() {
        assertThat(FreshserviceClient.mapPrioridad(Prioridad.CRITICA)).isEqualTo(4);
        assertThat(FreshserviceClient.mapPrioridad(Prioridad.ALTA)).isEqualTo(3);
        assertThat(FreshserviceClient.mapPrioridad(Prioridad.MEDIA)).isEqualTo(2);
        assertThat(FreshserviceClient.mapPrioridad(Prioridad.BAJA)).isEqualTo(1);
        assertThat(FreshserviceClient.mapPrioridad(null)).isEqualTo(2);
    }
}
