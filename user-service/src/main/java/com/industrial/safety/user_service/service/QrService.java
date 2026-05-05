package com.industrial.safety.user_service.service;

public interface QrService
{
    String generateAndUploadQr(String keycloakId, String fullName, String email, String role);
}
