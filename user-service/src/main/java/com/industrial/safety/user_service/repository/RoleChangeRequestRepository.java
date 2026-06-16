package com.industrial.safety.user_service.repository;

import com.industrial.safety.user_service.model.RoleChangeRequest;
import com.industrial.safety.user_service.model.RoleChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, String> {
    List<RoleChangeRequest> findByStatusOrderByCreatedAtDesc(RoleChangeStatus status);
    List<RoleChangeRequest> findAllByOrderByCreatedAtDesc();
}
