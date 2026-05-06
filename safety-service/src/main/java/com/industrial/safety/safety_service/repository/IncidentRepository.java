package com.industrial.safety.safety_service.repository;

import com.industrial.safety.safety_service.model.Incident;
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
}
