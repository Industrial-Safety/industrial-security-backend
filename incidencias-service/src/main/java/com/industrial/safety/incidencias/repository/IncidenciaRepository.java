package com.industrial.safety.incidencias.repository;

import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    List<Incidencia> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    List<Incidencia> findByEstado(EstadoIncidencia estado);

    List<Incidencia> findByPrioridad(Prioridad prioridad);
}
