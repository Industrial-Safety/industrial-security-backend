package com.industrial.safety.user_service.service;

import com.industrial.safety.user_service.dto.UserRequest;

public interface KeycloakService {
    String createUser(UserRequest userRequest);
    String getUserIdByEmail(String email);
    void assignRole(String keycloakId, String roleName);
    void updatePassword(String userId, String newPassword);
}
