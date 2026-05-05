package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
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
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;


    @Override
    public Map<String, String> generatePresignedUrl(String fileName, String contentType) {
        String key = "safety-images/" + fileName;

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

    @Override
    public String uploadFile(String key, byte[] fileBytes, String contentType) {
        String fullKey = "users/qr-codes/" + key;  // ← QR codes

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fullKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(fileBytes)
        );

        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fullKey);
    }
}
