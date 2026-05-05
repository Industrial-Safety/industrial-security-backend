package com.industrial.safety.user_service.service.Impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.industrial.safety.user_service.service.QrService;
import com.industrial.safety.user_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService
{
    private final StorageService storageService;

    @Override
    public String generateAndUploadQr(String keycloakId, String fullName, String email, String role) {
        try {
            // 1. Armamos el contenido del QR
            String qrContent = String.format(
                    "{\"keycloakId\":\"%s\",\"fullName\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
                    keycloakId, fullName, email, role
            );

            // 2. Generamos la imagen QR
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    300, 300
            );

            // 3. Convertimos a bytes PNG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();

            // 4. Delegamos la subida al StorageService ✅
            return storageService.uploadFile(
                    keycloakId + ".png",
                    qrBytes,
                    "image/png"
            );

        } catch (Exception e) {
            throw new RuntimeException("Error generando QR: " + e.getMessage());
        }
    }
}
