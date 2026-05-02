package com.industrial.safety.course_service.controller;

import com.industrial.safety.course_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        return ResponseEntity.ok(storageService.generatePresignedUrl(fileName, contentType));
    }
}

