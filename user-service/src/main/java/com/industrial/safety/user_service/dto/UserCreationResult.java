package com.industrial.safety.user_service.dto;

public record UserCreationResult(UserResponse user, boolean isNew) {}