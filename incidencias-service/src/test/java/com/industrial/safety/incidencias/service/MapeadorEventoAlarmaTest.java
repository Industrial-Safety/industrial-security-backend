package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.dto.AlarmaCloudWatch;
import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MapeadorEventoAlarma — alarma CloudWatch -> clasificación ITIL")
class MapeadorEventoAlarmaTest {

    private static AlarmaCloudWatch alarma(String name, String ns, String metrica, String razon) {
        return new AlarmaCloudWatch(name, "ALARM", razon, ns, metrica, 95.0);
    }

    @Test
    @DisplayName("CPU alta en ECS -> Infraestructura")
    void cpuInfra() {
        var a = alarma("ecs-cpu-high", "AWS/ECS", "CPUUtilization", "CPU > 95%");
        assertThat(MapeadorEventoAlarma.categoria(a)).isEqualTo(Categoria.INFRAESTRUCTURA);
    }

    @Test
    @DisplayName("RDS -> Base de datos")
    void rdsBd() {
        var a = alarma("rds-conn", "AWS/RDS", "DatabaseConnections", "muchas conexiones");
        assertThat(MapeadorEventoAlarma.categoria(a)).isEqualTo(Categoria.BASE_DATOS);
    }

    @Test
    @DisplayName("5xx del balanceador -> Aplicaciones")
    void http5xxApp() {
        var a = alarma("alb-5xx", "AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "muchos 5xx");
        assertThat(MapeadorEventoAlarma.categoria(a)).isEqualTo(Categoria.APLICACIONES);
    }

    @Test
    @DisplayName("Latencia / ELB -> Redes y comunicaciones")
    void latenciaRedes() {
        var a = alarma("alb-latency", "AWS/ApplicationELB", "TargetResponseTime", "latency alta");
        assertThat(MapeadorEventoAlarma.categoria(a)).isEqualTo(Categoria.REDES_COMUNICACIONES);
    }

    @Test
    @DisplayName("Texto 'critical/down' -> impacto ALTO; normal -> MEDIO")
    void severidad() {
        assertThat(MapeadorEventoAlarma.impacto(alarma("svc-down-critical", "AWS/ECS", "x", "service is down")))
                .isEqualTo(Nivel.ALTO);
        assertThat(MapeadorEventoAlarma.impacto(alarma("cpu-warn", "AWS/ECS", "CPUUtilization", "cpu 90%")))
                .isEqualTo(Nivel.MEDIO);
    }
}
