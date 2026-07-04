package com.industrial.safety.incidencias.dto;

/**
 * Datos relevantes de una alarma de CloudWatch (subconjunto del payload).
 * Se extrae del mensaje SNS o de un POST directo para el triaje de eventos.
 */
public record AlarmaCloudWatch(
        String alarmName,
        String estado,      // ALARM / OK / INSUFFICIENT_DATA (NewStateValue)
        String razon,       // NewStateReason
        String namespace,   // Trigger.Namespace, ej. AWS/ECS, AWS/RDS
        String metrica,     // Trigger.MetricName, ej. CPUUtilization
        Double umbral       // Trigger.Threshold
) {
}
