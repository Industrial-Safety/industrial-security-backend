package com.industrial.safety.course_service.repository;

import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PriceChangeRequestRepository extends MongoRepository<PriceChangeRequest, String> {
    List<PriceChangeRequest> findAllByOrderByCreatedAtDesc();
    List<PriceChangeRequest> findByStatusOrderByCreatedAtDesc(PriceChangeStatus status);
    List<PriceChangeRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
}
