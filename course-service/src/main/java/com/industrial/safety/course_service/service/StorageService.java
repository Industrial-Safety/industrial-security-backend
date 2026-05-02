package com.industrial.safety.course_service.service;

import java.util.Map;

public interface StorageService
{
    Map<String, String> generatePresignedUrl(String fileName, String contentType);
}
