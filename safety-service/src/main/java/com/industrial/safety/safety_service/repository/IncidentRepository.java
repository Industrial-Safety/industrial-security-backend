package com.industrial.safety.safety_service.repository;

import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String>{
    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    // Listar por cámara
    Page<Incident> findByCameraKey(String cameraKey, Pageable pageable);

    // Listar por cámara y estado
    Page<Incident> findByCameraKeyAndStatus(
            String cameraKey,
            IncidentStatus status,
            Pageable pageable
    );

    // Infracciones del propio trabajador
    Page<Incident> findByWorkerId(String workerId, Pageable pageable);

    // Apelaciones que debe resolver el jefe que aprobó la infracción
    Page<Incident> findByReviewedByAndAppealStatus(
            String reviewedBy,
            AppealStatus appealStatus,
            Pageable pageable
    );

    // Todas las apelaciones (cualquier estado) de los incidentes que aprobó el jefe
    Page<Incident> findByReviewedByAndAppealStatusIsNotNull(
            String reviewedBy,
            Pageable pageable
    );
}
