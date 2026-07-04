package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClasificadorReglas — fallback determinista de triaje")
class ClasificadorReglasTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("clasifica por síntomas derivados del catálogo del frontend")
    @CsvSource(textBlock = """
            No puedo crear cupones de descuento            | APLICACIONES
            No puedo subir el video del curso              | APLICACIONES
            No puedo iniciar sesión                        | SEGURIDAD
            La aplicación va muy lenta                     | REDES_COMUNICACIONES
            El servicio no está disponible                 | INFRAESTRUCTURA
            Se malogró la impresora de la oficina          | INFRAESTRUCTURA
            La cámara de detección está sin conexión       | INFRAESTRUCTURA
            No cargan los reportes de detección            | BASE_DATOS
            El reporte de inventario tiene datos incorrectos | BASE_DATOS
            El manual del procedimiento está desactualizado | DOCUMENTACION
            Error en el certificado del curso              | DOCUMENTACION
            asdfghjkl qwerty                               | OTROS
            """, delimiter = '|')
    void clasificaPorTexto(String descripcion, Categoria esperada) {
        assertThat(ClasificadorReglas.clasificar(descripcion.trim(), null).categoria())
                .isEqualTo(esperada);
    }

    @Test
    @DisplayName("el código HTTP del log manda: 403 -> SEGURIDAD aunque el texto hable de cupones")
    void codigoHttpDecisivo() {
        var s = ClasificadorReglas.clasificar(
                "No puedo crear cupones",
                "{\"status\":403,\"path\":\"/api/v1/cupones\"}");
        assertThat(s.categoria()).isEqualTo(Categoria.SEGURIDAD);
    }

    @Test
    @DisplayName("503 en el log -> INFRAESTRUCTURA con impacto ALTO (interrupción)")
    void codigo503EsCaida() {
        var s = ClasificadorReglas.clasificar("no responde", "{\"status\":503}");
        assertThat(s.categoria()).isEqualTo(Categoria.INFRAESTRUCTURA);
        assertThat(s.impacto()).isEqualTo(Nivel.ALTO);
    }

    @Test
    @DisplayName("500 genérico sin más señal -> APLICACIONES, impacto ALTO")
    void codigo500Generico() {
        var s = ClasificadorReglas.clasificar("algo raro pasó", "{\"status\":500}");
        assertThat(s.categoria()).isEqualTo(Categoria.APLICACIONES);
        assertThat(s.impacto()).isEqualTo(Nivel.ALTO);
    }

    @Test
    @DisplayName("sin señales: OTROS con impacto MEDIO y urgencia MEDIO")
    void sinSenales() {
        var s = ClasificadorReglas.clasificar("hola", null);
        assertThat(s.categoria()).isEqualTo(Categoria.OTROS);
        assertThat(s.impacto()).isEqualTo(Nivel.MEDIO);
        assertThat(s.urgencia()).isEqualTo(Nivel.MEDIO);
    }

    @Test
    @DisplayName("entradas nulas no explotan -> OTROS")
    void entradasNulas() {
        assertThat(ClasificadorReglas.clasificar(null, null).categoria())
                .isEqualTo(Categoria.OTROS);
    }
}
