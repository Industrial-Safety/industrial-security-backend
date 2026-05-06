package com.industrial.safety.user_service.service;

import java.util.Map;

public interface StorageService
{

    Map<String, String> generatePresignedUrl(String fileName, String contentType);
    String uploadFile(String key, byte[] fileBytes, String contentType);
}
