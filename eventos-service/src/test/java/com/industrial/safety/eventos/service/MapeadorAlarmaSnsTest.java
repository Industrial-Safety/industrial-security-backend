package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.config.UmbralProperties;
import com.industrial.safety.eventos.dto.Clasificacion;
import com.industrial.safety.eventos.entity.NivelEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MapeadorAlarmaSns — alarma CloudWatch -> evento del modulo")
class MapeadorAlarmaSnsTest {

    private final ClasificadorEventos clasificador =
            new ClasificadorEventos(new PoliticaUmbrales(new UmbralProperties()));

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "solicitudes-caido,                  solicitudes-service",
            "user-caido,                         user-service",
            "api-gateway-caido,                  api-gateway",
            "industrial-safety-keycloak-caido,   keycloak",
            "eventos-caido,                      eventos-service",
            "demo-incidencias,                   demo-incidencias-service"
    })
    void mapeaElServicioOrigen(String alarma, String esperado) {
        assertThat(MapeadorAlarmaSns.servicioOrigen(alarma)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("estado ALARM produce un mensaje que clasifica CRITICAL (escala a incidencia)")
    void alarmaClasificaCritical() {
        String msg = MapeadorAlarmaSns.mensaje("solicitudes-caido", "ALARM",
                "Threshold Crossed: 2 datapoints were less than the threshold (1.0)");
        Clasificacion c = clasificador.clasificar("RunningTaskCount", null, msg);
        assertThat(c.nivel()).isEqualTo(NivelEvento.CRITICAL);
        assertThat(c.nivel().generaIncidente()).isTrue();
    }

    @Test
    @DisplayName("estado OK produce un mensaje que clasifica INFORMACION (solo registro)")
    void recuperacionClasificaInformacion() {
        String msg = MapeadorAlarmaSns.mensaje("solicitudes-caido", "OK", null);
        Clasificacion c = clasificador.clasificar("RunningTaskCount", null, msg);
        assertThat(c.nivel()).isEqualTo(NivelEvento.INFORMACION);
        assertThat(c.nivel().generaIncidente()).isFalse();
    }
}
