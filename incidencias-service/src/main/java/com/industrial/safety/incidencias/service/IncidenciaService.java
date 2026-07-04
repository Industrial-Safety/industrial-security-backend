package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.dto.AlarmaCloudWatch;
import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.dto.ResolverIncidenciaRequest;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Prioridad;

import java.util.List;

public interface IncidenciaService {

    /** Crea una incidencia (estado REGISTRADO, prioridad calculada) y notifica al reportero. */
    IncidenciaResponse crear(CrearIncidenciaRequest request, String reporterId);

    /** Crea una incidencia a partir de una alarma de CloudWatch (fuente EVENTO, sin reportero humano). */
    IncidenciaResponse crearDesdeEvento(AlarmaCloudWatch alarma);

    /** Incidencias de un reportero (seguimiento), mas recientes primero. */
    List<IncidenciaResponse> misIncidencias(String reporterId);

    /** Todas las incidencias ordenadas por prioridad (CRITICA→BAJA) y luego fecha. Filtros opcionales. */
    List<IncidenciaResponse> listarTodas(EstadoIncidencia estado, Prioridad prioridad);

    IncidenciaResponse getById(Long id);

    /** El Admin/TI toma el caso: REGISTRADO → EN_ATENCION. */
    IncidenciaResponse aceptar(Long id, String adminId);

    /** El Admin/TI resuelve: EN_ATENCION → RESUELTO y notifica al reportero. */
    IncidenciaResponse resolver(Long id, ResolverIncidenciaRequest request, String adminId);

    /** El Admin/TI solicita sincronizar con Freshservice (async): marca PENDIENTE y encola. */
    IncidenciaResponse sincronizar(Long id, String adminId);

    /** Procesa la sincronización (lo invoca el consumidor). Propaga el error para reintento/DLQ. */
    void procesarSync(Long id);

    /** Marca la sincronización como fallida (deshabilitado o DLQ). */
    void marcarSyncError(Long id, String mensaje);

    /**
     * Procesa el triaje asistido por IA (lo invoca el consumidor de la cola de triaje).
     * Refina la clasificación por reglas con el resultado de la IA. Propaga el error
     * (p. ej. IA caída) para que se reintente y, si se agota, caiga a la DLQ.
     */
    void procesarTriaje(Long id);
}
