package com.industrial.safety.course_service.service.impl;

import com.industrial.safety.course_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public Map<String, String> generatePresignedUrl(String fileName, String contentType) {
        return buildPresignedUrl("safety-videos/" + fileName, contentType);
    }

    @Override
    public Map<String, String> generateCoverPresignedUrl(String fileName, String contentType) {
        return buildPresignedUrl("courses/covers/" + fileName, contentType);
    }

    private Map<String, String> buildPresignedUrl(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        String uploadUrl = presigned.url().toExternalForm();
        String fileUrl = presigned.url().toString().split("\\?")[0];
        return Map.of(
                "uploadUrl", uploadUrl,
                "fileUrl", fileUrl
        );
    }
}