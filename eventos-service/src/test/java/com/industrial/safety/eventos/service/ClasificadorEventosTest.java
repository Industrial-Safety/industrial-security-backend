package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.config.UmbralProperties;
import com.industrial.safety.eventos.dto.Clasificacion;
import com.industrial.safety.eventos.entity.Categoria;
import com.industrial.safety.eventos.entity.NivelEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClasificadorEventos — clasifica el timeline del material del curso")
class ClasificadorEventosTest {

    private final ClasificadorEventos clasificador =
            new ClasificadorEventos(new PoliticaUmbrales(new UmbralProperties()));

    @Test
    @DisplayName("CPU 72% -> INFORMACION / INFRAESTRUCTURA (no genera incidente)")
    void cpuNormal() {
        Clasificacion c = clasificador.clasificar("cpu", 72.0, "Uso de CPU al 72%");
        assertThat(c.nivel()).isEqualTo(NivelEvento.INFORMACION);
        assertThat(c.categoria()).isEqualTo(Categoria.INFRAESTRUCTURA);
        assertThat(c.nivel().generaIncidente()).isFalse();
    }

    @Test
    @DisplayName("Login fallido (25 intentos) -> ERROR / SEGURIDAD (genera incidente)")
    void loginFallido() {
        Clasificacion c = clasificador.clasificar("login_fallidos", 25.0, "25 intentos de login fallidos");
        assertThat(c.nivel()).isEqualTo(NivelEvento.ERROR);
        assertThat(c.categoria()).isEqualTo(Categoria.SEGURIDAD);
        assertThat(c.nivel().generaIncidente()).isTrue();
    }

    @Test
    @DisplayName("BD lenta (1800 ms) -> ERROR / BASE_DATOS")
    void bdLenta() {
        Clasificacion c = clasificador.clasificar("bd_latencia_ms", 1800.0, "Base de datos responde lentamente");
        assertThat(c.nivel()).isEqualTo(NivelEvento.ERROR);
        assertThat(c.categoria()).isEqualTo(Categoria.BASE_DATOS);
    }

    @Test
    @DisplayName("Disco 95% -> CRITICAL / INFRAESTRUCTURA")
    void discoLleno() {
        Clasificacion c = clasificador.clasificar("disco", 95.0, "Disco al 95%");
        assertThat(c.nivel()).isEqualTo(NivelEvento.CRITICAL);
        assertThat(c.categoria()).isEqualTo(Categoria.INFRAESTRUCTURA);
    }

    @Test
    @DisplayName("Servidor sin valor pero 'no responde' -> CRITICAL por palabra clave")
    void servidorDetenido() {
        Clasificacion c = clasificador.clasificar("servidor", null, "Servidor Web deja de responder");
        assertThat(c.nivel()).isEqualTo(NivelEvento.CRITICAL);
        assertThat(c.categoria()).isEqualTo(Categoria.INFRAESTRUCTURA);
    }

    @Test
    @DisplayName("Servicio recuperado -> INFORMACION por palabra clave")
    void servicioRecuperado() {
        Clasificacion c = clasificador.clasificar("servidor", null, "Servicio recuperado automaticamente");
        assertThat(c.nivel()).isEqualTo(NivelEvento.INFORMACION);
        assertThat(c.nivel().generaIncidente()).isFalse();
    }
}
