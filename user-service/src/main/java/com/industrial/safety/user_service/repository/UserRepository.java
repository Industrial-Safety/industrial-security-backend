package com.industrial.safety.user_service.repository;

import com.industrial.safety.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String>
{
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
}