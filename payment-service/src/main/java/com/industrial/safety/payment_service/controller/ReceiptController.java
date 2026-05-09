package com.industrial.safety.payment_service.controller;

import com.industrial.safety.payment_service.config.properties.ReceiptProperties;
import com.industrial.safety.payment_service.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/payments/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptProperties properties;

    @GetMapping("/{orderNumber}.pdf")
    public ResponseEntity<Resource> download(@PathVariable String orderNumber) {
        Path file = Path.of(properties.storage().outputDir()).resolve(orderNumber + ".pdf");
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new PaymentNotFoundException(orderNumber);
        }
        Resource resource = new FileSystemResource(file);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(orderNumber + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
