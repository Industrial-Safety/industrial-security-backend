package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.service.Impl.QrServiceImpl;
import com.industrial.safety.user_service.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrServiceImpl — Pruebas Unitarias")
class QrServiceImplTest {

    @Mock StorageService storageService;

    @InjectMocks QrServiceImpl qrService;

    @Test
    @DisplayName("generateAndUploadQr: genera QR, sube a S3 y retorna la URL")
    void generateAndUploadQr_success() {
        given(storageService.uploadFile(eq("kc-uuid-1.png"), any(byte[].class), eq("image/png")))
                .willReturn("https://bucket.s3.amazonaws.com/users/qr-codes/kc-uuid-1.png");

        String url = qrService.generateAndUploadQr(
                "kc-uuid-1", "Juan López", "juan@example.com", "ROLE_STUDENT"
        );

        assertThat(url).isEqualTo("https://bucket.s3.amazonaws.com/users/qr-codes/kc-uuid-1.png");
        then(storageService).should().uploadFile(eq("kc-uuid-1.png"), any(byte[].class), eq("image/png"));
    }

    @Test
    @DisplayName("generateAndUploadQr: si StorageService falla, lanza RuntimeException con mensaje claro")
    void generateAndUploadQr_storageThrows_wrapsException() {
        given(storageService.uploadFile(anyString(), any(byte[].class), anyString()))
                .willThrow(new RuntimeException("S3 connection refused"));

        assertThatThrownBy(() -> qrService.generateAndUploadQr(
                "kc-uuid-1", "Juan", "juan@example.com", "ROLE_STUDENT"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generando QR");
    }
}
