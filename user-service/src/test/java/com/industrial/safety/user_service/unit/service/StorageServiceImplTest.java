package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.service.Impl.StorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorageServiceImpl — Pruebas Unitarias")
class StorageServiceImplTest {

    @Mock S3Presigner s3Presigner;
    @Mock S3Client s3Client;
    @Mock PresignedPutObjectRequest presigned;

    @InjectMocks StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("generatePresignedUrl: devuelve uploadUrl y fileUrl (sin query)")
    void generatePresignedUrl_returnsUrls() throws Exception {
        URL url = new URL("https://test-bucket.s3.amazonaws.com/safety-images/foto.png?X-Amz-Signature=abc");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);
        given(presigned.url()).willReturn(url);

        Map<String, String> result = storageService.generatePresignedUrl("foto.png", "image/png");

        assertThat(result.get("uploadUrl")).contains("X-Amz-Signature");
        assertThat(result.get("fileUrl")).doesNotContain("?");
        assertThat(result.get("fileUrl")).endsWith("safety-images/foto.png");
    }

    @Test
    @DisplayName("uploadFile: sube a S3 y devuelve la URL pública")
    void uploadFile_putsObjectAndReturnsUrl() {
        String url = storageService.uploadFile("qr-1.png", new byte[]{1, 2, 3}, "image/png");

        then(s3Client).should().putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertThat(url).isEqualTo("https://test-bucket.s3.amazonaws.com/users/qr-codes/qr-1.png");
    }
}
