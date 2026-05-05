package com.industrial.safety.user_service.service;

import com.industrial.safety.user_service.dto.UserRequest;

public interface KeycloakService {
    String createUser(UserRequest userRequest);
}
