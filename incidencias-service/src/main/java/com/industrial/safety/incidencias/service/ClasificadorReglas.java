package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clasificador determinista de respaldo del triaje (fallback).
 *
 * <p>Lee la descripción del usuario + el log capturado del navegador y propone
 * categoría/impacto/urgencia por palabras clave y códigos HTTP. NO reemplaza a la IA:
 * es la red de seguridad que garantiza que TODA incidencia quede clasificada al
 * instante aunque la IA esté caída; la IA luego la refina de forma asíncrona.
 *
 * <p>El diccionario de síntomas se derivó del catálogo del frontend
 * ({@code features/incidencias/catalog.ts}) para que reglas y catálogo compartan taxonomía.
 *
 * <p>Clase utilitaria pura: sin estado y testeable de forma aislada, como {@link PrioridadCalculator}.
 */
public final class ClasificadorReglas {

    /** Resultado del triaje por reglas: categoría + niveles provisionales. */
    public record Sugerencia(Categoria categoria, Nivel impacto, Nivel urgencia) {
    }

    private ClasificadorReglas() {
    }

    /** Grupos de palabras clave por categoría, evaluados EN ORDEN (el primero que matchea gana). */
    private static final List<Map.Entry<Categoria, List<String>>> REGLAS = List.of(
            Map.entry(Categoria.SEGURIDAD, List.of(
                    "iniciar sesion", "sesion", "login", "contrasena", "password",
                    "permiso", "acceso", "autentic", "bloquead", "credencial")),
            Map.entry(Categoria.REDES_COMUNICACIONES, List.of(
                    "lent", "demor", "timeout", "time out", "internet", "wifi",
                    "redes", "latencia", "no responde", "intermitente")),
            Map.entry(Categoria.INFRAESTRUCTURA, List.of(
                    "caid", "no disponible", "no esta disponible", "fuera de servicio", "servidor", "almacenamiento",
                    "camar", "sensor", "lector", "escaner", "impresora", "proyector",
                    "hardware", "equipo", "dispositivo", "sin conexion",
                    "no enciende", "no prende", "se malogr", "averi")),
            Map.entry(Categoria.BASE_DATOS, List.of(
                    "reporte", "inventario", "datos incorrectos", "dato incorrecto",
                    "informacion incorrecta", "base de datos", "duplicad", "consulta")),
            Map.entry(Categoria.DOCUMENTACION, List.of(
                    "document", "manual", "certificad", "constancia", "material",
                    "procedimiento", "normativa", "guia", "instructivo", "desactualizad")),
            Map.entry(Categoria.APLICACIONES, List.of(
                    "curso", "video", "foro", "pago", "cupon", "descuento", "solicitud",
                    "apel", "infraccion", "alerta", "aprobar", "crear", "editar", "subir",
                    "boton", "guardar", "formulario", "precio", "compra", "epp", "carga", "carrito"))
    );

    private static final Pattern CODIGO_HTTP = Pattern.compile("\\b([1-5]\\d{2})\\b");

    private static final List<String> SENALES_INTERRUPCION = List.of(
            "caid", "no disponible", "no esta disponible", "fuera de servicio", "no funciona", "sin conexion");

    public static Sugerencia clasificar(String descripcion, String contextoError) {
        String texto = normalizar(safe(descripcion) + " " + safe(contextoError));
        Optional<Integer> codigo = codigoHttp(texto);

        Categoria categoria = porCodigoDecisivo(codigo)
                .or(() -> porPalabrasClave(texto))
                .or(() -> porCodigoDebil(codigo))
                .orElse(Categoria.OTROS);

        Nivel impacto = esInterrupcion(texto, codigo) ? Nivel.ALTO : Nivel.MEDIO;
        return new Sugerencia(categoria, impacto, Nivel.MEDIO);
    }

    /** Códigos HTTP inequívocos: 401/403 = seguridad, 502/503/504 = infraestructura caída. */
    private static Optional<Categoria> porCodigoDecisivo(Optional<Integer> codigo) {
        return codigo.flatMap(c -> switch (c) {
            case 401, 403 -> Optional.of(Categoria.SEGURIDAD);
            case 502, 503, 504 -> Optional.of(Categoria.INFRAESTRUCTURA);
            default -> Optional.empty();
        });
    }

    /** Cualquier otro 4xx/5xx sin señal más específica: error genérico de la app. */
    private static Optional<Categoria> porCodigoDebil(Optional<Integer> codigo) {
        return codigo.filter(c -> c >= 400).map(c -> Categoria.APLICACIONES);
    }

    private static Optional<Categoria> porPalabrasClave(String texto) {
        return REGLAS.stream()
                .filter(regla -> regla.getValue().stream().anyMatch(clave -> contiene(texto, clave)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean esInterrupcion(String texto, Optional<Integer> codigo) {
        boolean caidaPorTexto = SENALES_INTERRUPCION.stream().anyMatch(s -> contiene(texto, s));
        boolean caidaPorCodigo = codigo.filter(c -> c >= 500).isPresent();
        return caidaPorTexto || caidaPorCodigo;
    }

    private static Optional<Integer> codigoHttp(String texto) {
        Matcher m = CODIGO_HTTP.matcher(texto);
        return m.find() ? Optional.of(Integer.parseInt(m.group(1))) : Optional.empty();
    }

    /**
     * Frases (con espacio) se buscan como substring; palabras sueltas se buscan como
     * prefijo de token (así "lent" matchea "lento/lenta", "cupon" → "cupones", etc.).
     */
    private static boolean contiene(String texto, String clave) {
        if (clave.indexOf(' ') >= 0) {
            return texto.contains(clave);
        }
        for (String token : texto.split("[^a-z0-9]+")) {
            if (token.startsWith(clave)) {
                return true;
            }
        }
        return false;
    }

    /** minúsculas + sin tildes, para comparar sin sorpresas de acentuación. */
    private static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
