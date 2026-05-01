package com.industrial.safety.course_service.service;

public interface StorageService
{
    String generatePresignedUrl(String fileName,String contenyType);
}
