package com.industrial.safety.eventos.service;

import com.industrial.safety.eventos.dto.EventoResponse;
import com.industrial.safety.eventos.dto.RegistrarEventoRequest;
import com.industrial.safety.eventos.entity.NivelEvento;

import java.util.List;

public interface EventoService {

    /** Registra un evento: lo clasifica por umbrales y, si es ERROR/CRITICAL, lo escala a incidencia. */
    EventoResponse registrar(RegistrarEventoRequest request);

    /** Lista eventos (mas recientes primero) con filtros opcionales por nivel y/o servicio. */
    List<EventoResponse> listar(NivelEvento nivel, String servicioOrigen);

    EventoResponse getById(Long id);

    /**
     * Carga el timeline de ejemplo del material del curso (CPU 72% → RAM 85% → Login fallido →
     * BD lenta → Disco 95% → Servidor detenido → Servicio recuperado) ya clasificado.
     * Sirve para demostrar el modulo en clase.
     */
    List<EventoResponse> cargarDemo();
}
