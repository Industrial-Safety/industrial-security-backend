package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.dto.Clasificacion;
import com.industrial.safety.eventos.entity.Categoria;
import com.industrial.safety.eventos.entity.NivelEvento;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

/**
 * Clasifica un evento en {@link NivelEvento} (Informacion/Warning/Error/Critical) y
 * {@link Categoria}, siguiendo el marco del curso (S15/S29).
 *
 * <ol>
 *   <li>Si hay valor numerico y una politica de umbral para la metrica, decide por
 *       {@link PoliticaUmbrales} (diapositiva 30).</li>
 *   <li>Si no (evento textual, ej. "servidor detenido"), decide por palabras clave.</li>
 * </ol>
 *
 * La categoria sale de la metrica + el mensaje (misma taxonomia que incidencias-service).
 */
@Component
public class ClasificadorEventos {

    private final PoliticaUmbrales politica;

    public ClasificadorEventos(PoliticaUmbrales politica) {
        this.politica = politica;
    }

    public Clasificacion clasificar(String metrica, Double valor, String mensaje) {
        Categoria categoria = categoria(metrica, mensaje);

        if (valor != null && politica.tieneUmbral(metrica)) {
            NivelEvento nivel = politica.evaluar(metrica, valor).orElse(NivelEvento.INFORMACION);
            return new Clasificacion(nivel, categoria, politica.describir(metrica, valor, nivel));
        }

        return porTexto(metrica, mensaje, categoria);
    }

    // ── Nivel por palabras clave (eventos textuales / sin politica numerica) ─────

    private static final List<String> INTERRUPCION = List.of(
            "detenid", "caid", "down", "apagad", "no responde", "deja de responder", "dejo de responder",
            "fuera de servicio", "interrump", "outage", "unavailable", "no disponible", "sin conexion", "fatal");
    private static final List<String> DEGRADACION = List.of(
            "lent", "latencia", "degrad", "falla", "error", "timeout", "time out", "5xx", "excedid");
    private static final List<String> RIESGO = List.of(
            "warning", "riesgo", "elevad", "alto", "reinici", "reintent", "sospech", "4xx", "umbral");
    private static final List<String> NORMALIDAD = List.of(
            "recuper", "restaur", "exitos", "normal", "disponible", "iniciad", "arranc", "ok", "saludable");

    private Clasificacion porTexto(String metrica, String mensaje, Categoria categoria) {
        String texto = normalizar(safe(metrica) + " " + safe(mensaje));
        if (contieneAlguna(texto, INTERRUPCION)) {
            return new Clasificacion(NivelEvento.CRITICAL, categoria, "palabra clave -> CRITICAL");
        }
        if (contieneAlguna(texto, DEGRADACION)) {
            return new Clasificacion(NivelEvento.ERROR, categoria, "palabra clave -> ERROR");
        }
        if (contieneAlguna(texto, RIESGO)) {
            return new Clasificacion(NivelEvento.WARNING, categoria, "palabra clave -> WARNING");
        }
        if (contieneAlguna(texto, NORMALIDAD)) {
            return new Clasificacion(NivelEvento.INFORMACION, categoria, "palabra clave -> INFORMACION");
        }
        return new Clasificacion(NivelEvento.INFORMACION, categoria, "sin senal -> INFORMACION");
    }

    // ── Categoria por metrica + mensaje (taxonomia ITIL del curso) ───────────────

    static Categoria categoria(String metrica, String mensaje) {
        String t = normalizar(safe(metrica) + " " + safe(mensaje));
        if (contiene(t, "login") || contiene(t, "autentic") || contiene(t, "password")
                || contiene(t, "contrasena") || contiene(t, "sesion") || contiene(t, "credencial")
                || contiene(t, "acceso") || contiene(t, "seguridad")) {
            return Categoria.SEGURIDAD;
        }
        if (contiene(t, "bd") || contiene(t, "base de datos") || contiene(t, "database")
                || contiene(t, "rds") || contiene(t, "sql") || contiene(t, "consulta")) {
            return Categoria.BASE_DATOS;
        }
        if (contiene(t, "cpu") || contiene(t, "ram") || contiene(t, "memoria") || contiene(t, "memory")
                || contiene(t, "disco") || contiene(t, "disk") || contiene(t, "servidor")
                || contiene(t, "server") || contiene(t, "almacenamiento") || contiene(t, "infra")) {
            return Categoria.INFRAESTRUCTURA;
        }
        if (contiene(t, "latencia") || contiene(t, "latency") || contiene(t, "red")
                || contiene(t, "network") || contiene(t, "timeout") || contiene(t, "conexion")) {
            return Categoria.REDES_COMUNICACIONES;
        }
        if (contiene(t, "http") || contiene(t, "5xx") || contiene(t, "4xx") || contiene(t, "app")
                || contiene(t, "endpoint") || contiene(t, "api")) {
            return Categoria.APLICACIONES;
        }
        return Categoria.OTROS;
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private static boolean contieneAlguna(String texto, List<String> claves) {
        return claves.stream().anyMatch(c -> texto.contains(c));
    }

    private static boolean contiene(String texto, String clave) {
        return texto.contains(clave);
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
