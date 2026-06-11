package com.industrial.safety.course_service.unit.controller;

import com.industrial.safety.course_service.controller.StorageController;
import com.industrial.safety.course_service.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorageController — Pruebas Unitarias")
class StorageControllerTest {

    @Mock StorageService storageService;

    @InjectMocks StorageController storageController;

    @Test
    @DisplayName("getUploadUrl: delega a StorageService y retorna 200 con mapa de URLs")
    void getUploadUrl_returns200WithUrls() {
        Map<String, String> expected = Map.of(
                "uploadUrl", "https://bucket.s3.amazonaws.com/safety-videos/v.mp4?sig=abc",
                "fileUrl",   "https://bucket.s3.amazonaws.com/safety-videos/v.mp4"
        );
        given(storageService.generatePresignedUrl("v.mp4", "video/mp4")).willReturn(expected);

        ResponseEntity<Map<String, String>> response =
                storageController.getUploadUrl("v.mp4", "video/mp4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        then(storageService).should().generatePresignedUrl("v.mp4", "video/mp4");
        then(storageService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("getCoverUploadUrl: delega a StorageService y retorna 200 con mapa de URLs")
    void getCoverUploadUrl_returns200WithUrls() {
        Map<String, String> expected = Map.of(
                "uploadUrl", "https://bucket.s3.amazonaws.com/courses/covers/c.jpg?sig=xyz",
                "fileUrl",   "https://bucket.s3.amazonaws.com/courses/covers/c.jpg"
        );
        given(storageService.generateCoverPresignedUrl("c.jpg", "image/jpeg")).willReturn(expected);

        ResponseEntity<Map<String, String>> response =
                storageController.getCoverUploadUrl("c.jpg", "image/jpeg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        then(storageService).should().generateCoverPresignedUrl("c.jpg", "image/jpeg");
        then(storageService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("getUploadUrl: pasa fileName y contentType exactos al servicio")
    void getUploadUrl_passesParamsToService() {
        given(storageService.generatePresignedUrl(anyString(), anyString()))
                .willReturn(Map.of("uploadUrl", "u", "fileUrl", "f"));

        storageController.getUploadUrl("doc.pdf", "application/pdf");

        then(storageService).should().generatePresignedUrl("doc.pdf", "application/pdf");
    }
}
