package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.dto.AlarmaCloudWatch;
import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;

import java.text.Normalizer;

/**
 * Traduce una alarma de CloudWatch (evento) a la clasificación ITIL de una incidencia.
 * La categoría sale del namespace/métrica; el impacto de la severidad del texto de la alarma.
 *
 * <p>Clase utilitaria pura, testeable de forma aislada (como {@link ClasificadorReglas}).
 */
public final class MapeadorEventoAlarma {

    private MapeadorEventoAlarma() {
    }

    public static Categoria categoria(AlarmaCloudWatch a) {
        String ns = safe(a.namespace()).toUpperCase();
        String texto = normalizar(safe(a.alarmName()) + " " + safe(a.metrica()) + " " + safe(a.razon()));
        if (ns.contains("RDS") || texto.contains("database") || texto.contains("rds") || texto.contains("base de datos")) {
            return Categoria.BASE_DATOS;
        }
        // El código HTTP manda antes que el namespace del balanceador (un 5xx es de la app, no de red).
        if (texto.contains("5xx") || texto.contains("4xx") || texto.contains("httpcode")) {
            return Categoria.APLICACIONES;
        }
        if (ns.contains("ELB") || texto.contains("latency") || texto.contains("latencia")
                || texto.contains("network") || texto.contains("responsetime") || texto.contains("response time")) {
            return Categoria.REDES_COMUNICACIONES;
        }
        if (ns.contains("ECS") || ns.contains("EC2") || texto.contains("cpu") || texto.contains("memory")
                || texto.contains("memoria") || texto.contains("disk") || texto.contains("disco")) {
            return Categoria.INFRAESTRUCTURA;
        }
        return Categoria.OTROS;
    }

    /** ALTO si la alarma sugiere severidad crítica/caída; MEDIO en el resto. */
    public static Nivel impacto(AlarmaCloudWatch a) {
        String t = normalizar(safe(a.alarmName()) + " " + safe(a.razon()));
        boolean critico = t.contains("critical") || t.contains("critico") || t.contains("down")
                || t.contains("caido") || t.contains("unavailable") || t.contains("no disponible")
                || t.contains("fatal") || t.contains("outage");
        return critico ? Nivel.ALTO : Nivel.MEDIO;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
