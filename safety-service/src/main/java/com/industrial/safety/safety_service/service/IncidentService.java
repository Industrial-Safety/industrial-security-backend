package com.industrial.safety.safety_service.service;

import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentService {
    IncidentResponse create(CreateIncidentRequest request);

    IncidentResponse review(String id, ReviewIncidentRequest request, String reviewerId);

    Page<IncidentResponse> list(IncidentStatus status, String cameraKey, Pageable pageable);

    IncidentResponse findById(String id);
}
