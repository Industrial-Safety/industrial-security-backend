package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorageServiceImpl — Pruebas Unitarias")
class StorageServiceImplTest {

    @Mock S3Presigner                s3Presigner;
    @Mock PresignedPutObjectRequest  presignedRequest;

    @InjectMocks StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("generatePresignedUrl: retorna mapa con uploadUrl y fileUrl para videos")
    void generatePresignedUrl_returnsUrls() throws Exception {
        URL url = new URL(
                "https://test-bucket.s3.amazonaws.com/safety-videos/video.mp4?X-Amz-Signature=abc");
        given(presignedRequest.url()).willReturn(url);
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedRequest);

        Map<String, String> result = storageService.generatePresignedUrl("video.mp4", "video/mp4");

        assertThat(result).containsKeys("uploadUrl", "fileUrl");
        assertThat(result.get("uploadUrl")).contains("X-Amz-Signature");
        assertThat(result.get("fileUrl")).doesNotContain("?");
    }

    @Test
    @DisplayName("generateCoverPresignedUrl: retorna mapa con uploadUrl y fileUrl para portadas")
    void generateCoverPresignedUrl_returnsUrls() throws Exception {
        URL url = new URL(
                "https://test-bucket.s3.amazonaws.com/courses/covers/cover.jpg?X-Amz-Signature=xyz");
        given(presignedRequest.url()).willReturn(url);
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedRequest);

        Map<String, String> result = storageService.generateCoverPresignedUrl("cover.jpg", "image/jpeg");

        assertThat(result).containsKeys("uploadUrl", "fileUrl");
        assertThat(result.get("fileUrl")).doesNotContain("?");
    }

    @Test
    @DisplayName("fallbackPresignedUrl: lanza S3UnavailableException con mensaje descriptivo")
    void fallbackPresignedUrl_throwsS3UnavailableException() throws Exception {
        // ReflectionTestUtils.invokeMethod no encuentra el método porque infiere RuntimeException.class
        // en lugar de Throwable.class; usamos reflexión directa con el tipo declarado.
        Method m = StorageServiceImpl.class.getDeclaredMethod(
                "fallbackPresignedUrl", String.class, String.class, Throwable.class);
        m.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                m.invoke(storageService, "video.mp4", "video/mp4", new RuntimeException("timeout"));
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException re) throw re;
                throw new RuntimeException(cause);
            }
        })
        .isInstanceOf(StorageServiceImpl.S3UnavailableException.class)
        .hasMessageContaining("almacenamiento");
    }
}
