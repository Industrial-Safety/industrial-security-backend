package com.industrial.safety.eventos.service;

/**
 * Traduce una alarma de CloudWatch (via SNS) a los campos de un evento del modulo.
 *
 * <p>Las alarmas de caida siguen la convencion {@code <servicio>-caido} (ej.
 * {@code solicitudes-caido} → {@code solicitudes-service}); el nivel lo decide
 * {@link ClasificadorEventos} por palabras clave del mensaje ("caído" → CRITICAL,
 * "recuperado" → INFORMACION), de modo que el flujo respeta el marco del curso:
 * el evento se registra SIEMPRE (historico) y solo Error/Critical escalan a incidencia.
 *
 * <p>Clase utilitaria pura, testeable de forma aislada.
 */
public final class MapeadorAlarmaSns {

    private MapeadorAlarmaSns() {
    }

    /** Nombre del servicio a partir del nombre de la alarma ({@code user-caido} → {@code user-service}). */
    public static String servicioOrigen(String alarmName) {
        if (alarmName == null || alarmName.isBlank()) {
            return "monitoreo";
        }
        String base = alarmName.trim().toLowerCase().replaceAll("-caido$", "");
        if (base.equals("api-gateway")) {
            return "api-gateway";
        }
        if (base.contains("keycloak")) {
            return "keycloak";
        }
        return base.endsWith("-service") ? base : base + "-service";
    }

    /**
     * Mensaje legible del evento segun el estado de la alarma (decide el nivel por palabras clave).
     * OJO: en la recuperacion NO se incluye el nombre crudo de la alarma ("x-caido"):
     * la palabra "caido" haria que el clasificador lo marque CRITICAL por error.
     */
    public static String mensaje(String alarmName, String estado, String razon) {
        if ("ALARM".equalsIgnoreCase(estado)) {
            String detalle = razon == null || razon.isBlank() ? "sin detalle" : razon.trim();
            return "Servicio caído — alarma " + alarmName + ": " + detalle;
        }
        return "Servicio recuperado — la alarma de " + servicioOrigen(alarmName) + " volvió a OK";
    }
}
