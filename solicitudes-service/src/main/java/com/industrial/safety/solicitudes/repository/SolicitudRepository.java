package com.industrial.safety.solicitudes.repository;

import com.industrial.safety.solicitudes.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByTipoIgnoreCase(String tipo);
}
