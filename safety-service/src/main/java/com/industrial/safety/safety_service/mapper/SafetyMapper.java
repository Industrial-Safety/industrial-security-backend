package com.industrial.safety.safety_service.mapper;

import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SafetyMapper {

    WorkerComplianceScoreResponse toResponse(WorkerComplianceScore score);
}
