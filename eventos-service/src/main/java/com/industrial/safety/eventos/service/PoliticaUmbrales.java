package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.config.UmbralProperties;
import com.industrial.safety.eventos.entity.NivelEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Motor de umbrales: traduce (metrica, valor) al {@link NivelEvento} correspondiente
 * segun las politicas de deteccion del curso (diapositiva 30).
 *
 * <p>Ejemplo CPU: 60-75 Informacion, 75-85 Warning, 85-95 Error, 95%+ Critical.
 * Cada metrica se modela como bandas ordenadas por su limite inferior; a un valor
 * observado se le asigna la banda de mayor {@code desde} que sea &le; al valor.
 *
 * <p>Los defaults viven aqui y se pueden sobreescribir/ampliar por configuracion
 * ({@link UmbralProperties}) sin recompilar.
 */
@Slf4j
@Component
public class PoliticaUmbrales {

    /** metrica normalizada -> (limite inferior -> nivel). */
    private final Map<String, NavigableMap<Double, NivelEvento>> politicas;

    public PoliticaUmbrales(UmbralProperties props) {
        this.politicas = construir(props);
    }

    /** Politicas por defecto (material S15/S29). */
    private static Map<String, NavigableMap<Double, NivelEvento>> defaults() {
        Map<String, NavigableMap<Double, NivelEvento>> m = new LinkedHashMap<>();
        m.put("cpu", banda(0, NivelEvento.INFORMACION, 75, NivelEvento.WARNING, 85, NivelEvento.ERROR, 95, NivelEvento.CRITICAL));
        m.put("ram", banda(0, NivelEvento.INFORMACION, 75, NivelEvento.WARNING, 85, NivelEvento.ERROR, 95, NivelEvento.CRITICAL));
        m.put("disco", banda(0, NivelEvento.INFORMACION, 80, NivelEvento.WARNING, 90, NivelEvento.ERROR, 95, NivelEvento.CRITICAL));
        m.put("login_fallidos", banda(0, NivelEvento.INFORMACION, 5, NivelEvento.WARNING, 15, NivelEvento.ERROR, 30, NivelEvento.CRITICAL));
        m.put("bd_latencia_ms", banda(0, NivelEvento.INFORMACION, 500, NivelEvento.WARNING, 1500, NivelEvento.ERROR, 5000, NivelEvento.CRITICAL));
        return m;
    }

    /** true si hay una politica de umbral numerico para la metrica. */
    public boolean tieneUmbral(String metrica) {
        return politicas.containsKey(normalizar(metrica));
    }

    /** Nivel para (metrica, valor) segun la banda aplicable; vacio si no hay politica. */
    public Optional<NivelEvento> evaluar(String metrica, double valor) {
        NavigableMap<Double, NivelEvento> bandas = politicas.get(normalizar(metrica));
        if (bandas == null) {
            return Optional.empty();
        }
        Map.Entry<Double, NivelEvento> e = bandas.floorEntry(valor);
        // Por debajo de la banda mas baja definida: se considera Informacion.
        return Optional.of(e != null ? e.getValue() : NivelEvento.INFORMACION);
    }

    /** Descripcion legible del umbral aplicado, ej. "cpu >= 95 -> CRITICAL". */
    public String describir(String metrica, double valor, NivelEvento nivel) {
        NavigableMap<Double, NivelEvento> bandas = politicas.get(normalizar(metrica));
        Double desde = bandas != null ? bandas.floorKey(valor) : null;
        return desde != null
                ? "%s >= %s -> %s".formatted(normalizar(metrica), formato(desde), nivel)
                : "%s=%s -> %s".formatted(normalizar(metrica), formato(valor), nivel);
    }

    /** Vista de solo lectura de las politicas activas (para exponerlas en el API). */
    public Map<String, NavigableMap<Double, NivelEvento>> vista() {
        return politicas;
    }

    // ── internos ──────────────────────────────────────────────────────

    private static Map<String, NavigableMap<Double, NivelEvento>> construir(UmbralProperties props) {
        Map<String, NavigableMap<Double, NivelEvento>> m = defaults();
        if (props != null && props.getUmbrales() != null) {
            props.getUmbrales().forEach((metrica, bandas) -> {
                NavigableMap<Double, NivelEvento> parsed = parsear(metrica, bandas);
                if (!parsed.isEmpty()) {
                    m.put(normalizar(metrica), parsed);
                }
            });
        }
        return m;
    }

    /** Parsea una lista de "desde:NIVEL" a un mapa ordenado; ignora entradas invalidas. */
    private static NavigableMap<Double, NivelEvento> parsear(String metrica, java.util.List<String> bandas) {
        NavigableMap<Double, NivelEvento> mapa = new TreeMap<>();
        if (bandas == null) {
            return mapa;
        }
        for (String banda : bandas) {
            String[] partes = banda.split(":");
            if (partes.length != 2) {
                log.warn("[umbrales] banda invalida en '{}': '{}' (se ignora)", metrica, banda);
                continue;
            }
            try {
                mapa.put(Double.parseDouble(partes[0].trim()), NivelEvento.valueOf(partes[1].trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                log.warn("[umbrales] banda invalida en '{}': '{}' ({})", metrica, banda, ex.getMessage());
            }
        }
        return mapa;
    }

    private static NavigableMap<Double, NivelEvento> banda(Object... pares) {
        NavigableMap<Double, NivelEvento> m = new TreeMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put(((Number) pares[i]).doubleValue(), (NivelEvento) pares[i + 1]);
        }
        return m;
    }

    private static String formato(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /** minusculas + alias de metricas comunes hacia la clave canonica. */
    static String normalizar(String metrica) {
        if (metrica == null) {
            return "";
        }
        String base = metrica.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return switch (base) {
            case "cpu_utilization", "cpuutilization", "uso_cpu", "cpu_porcentaje" -> "cpu";
            case "memory", "memoria", "mem", "uso_ram", "memory_utilization" -> "ram";
            case "disk", "disco_uso", "disk_utilization", "almacenamiento" -> "disco";
            case "login_fallido", "logins_fallidos", "intentos_login", "failed_logins" -> "login_fallidos";
            case "bd_latencia", "db_latency", "latencia_bd", "latencia_db_ms", "db_latency_ms" -> "bd_latencia_ms";
            default -> base;
        };
    }
}
