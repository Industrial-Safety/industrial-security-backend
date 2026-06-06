package com.industrial.safety.exam_service.unit.pdf;

import com.industrial.safety.exam_service.pdf.CertificatePdfGenerator;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CertificatePdfGenerator — Pruebas Unitarias")
class CertificatePdfGeneratorTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock PresignedGetObjectRequest presigned;
    @InjectMocks CertificatePdfGenerator generator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generator, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("presignUrl: devuelve URL presignada")
    void presignUrl_returnsUrl() throws Exception {
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);
        given(presigned.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/certificates/1/s1.pdf?sig=x"));

        assertThat(generator.presignUrl("certificates/1/s1.pdf")).startsWith("https://");
    }

    @Test
    @DisplayName("generateAndUpload: renderiza el PDF y lo sube a S3")
    void generateAndUpload_rendersAndUploads() {
        String key = generator.generateAndUpload(
                "student-123", 1L, "Juan Pérez", "Seguridad Industrial", "Prof. Nieto", 95);

        assertThat(key).isEqualTo("certificates/1/student-123.pdf");
        then(s3Client).should().putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("generateAndUpload: id corto y nombres null (escape + buildCode)")
    void generateAndUpload_shortIdAndNulls() {
        String key = generator.generateAndUpload("s1", 2L, null, null, null, 50);
        assertThat(key).isEqualTo("certificates/2/s1.pdf");
    }
}
