package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StorageServiceImpl (course) — Pruebas Unitarias")
class StorageServiceImplTest {

    @Mock S3Presigner s3Presigner;
    @Mock PresignedPutObjectRequest presigned;
    @InjectMocks StorageServiceImpl storageService;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
        URL url = new URL("https://test-bucket.s3.amazonaws.com/key/file.mp4?X-Amz-Signature=abc");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);
        given(presigned.url()).willReturn(url);
    }

    @Test
    @DisplayName("generatePresignedUrl: devuelve uploadUrl y fileUrl")
    void generatePresignedUrl() {
        Map<String, String> result = storageService.generatePresignedUrl("file.mp4", "video/mp4");
        assertThat(result).containsKeys("uploadUrl", "fileUrl");
        assertThat(result.get("fileUrl")).doesNotContain("?");
    }

    @Test
    @DisplayName("generateCoverPresignedUrl: devuelve uploadUrl y fileUrl")
    void generateCoverPresignedUrl() {
        Map<String, String> result = storageService.generateCoverPresignedUrl("cover.png", "image/png");
        assertThat(result).containsKeys("uploadUrl", "fileUrl");
    }
}
