package com.industrial.safety.exam_service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificatePdfGenerator {

    private static final String S3_KEY_PREFIX = "certificates/";
    private static final Duration PRESIGN_TTL = Duration.ofDays(7);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd 'de' MMMM 'de' yyyy")
            .withZone(ZoneId.systemDefault())
            .withLocale(new java.util.Locale("es", "ES"));

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @CircuitBreaker(name = "aws-s3", fallbackMethod = "fallbackGenerateAndUpload")
    @Retry(name = "aws-s3")
    public String generateAndUpload(String studentId, Long examId,
                                    String studentName, String courseName,
                                    String instructorName, Integer score) {
        byte[] pdf = render(studentName, courseName, instructorName, score);
        String key = S3_KEY_PREFIX + examId + "/" + studentId + ".pdf";

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .contentDisposition("inline; filename=\"certificate.pdf\"")
                .build();
        s3Client.putObject(put, RequestBody.fromBytes(pdf));
        log.info("Certificate uploaded to s3://{}/{}", bucketName, key);
        return key;
    }

    @CircuitBreaker(name = "aws-s3", fallbackMethod = "fallbackPresignUrl")
    @Retry(name = "aws-s3")
    public String presignUrl(String key) {
        return presign(key);
    }

    // --- Fallbacks ---

    @SuppressWarnings("unused")
    private String fallbackGenerateAndUpload(String studentId, Long examId,
                                             String studentName, String courseName,
                                             String instructorName, Integer score,
                                             Throwable ex) {
        log.error("AWS S3 circuit open — no se pudo subir certificado para estudiante={}, examen={}: {}",
                studentId, examId, ex.getMessage());
        throw new S3UnavailableException("Servicio de almacenamiento no disponible. El certificado no pudo generarse.");
    }

    @SuppressWarnings("unused")
    private String fallbackPresignUrl(String key, Throwable ex) {
        log.error("AWS S3 circuit open — no se pudo generar URL para certificado {}: {}", key, ex.getMessage());
        return "";
    }

    // --- Helpers ---

    private String presign(String key) {
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_TTL)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(req);
        return presigned.url().toExternalForm();
    }

    private byte[] render(String studentName, String courseName,
                          String instructorName, Integer score) {
        // Landscape A4: 842 x 595 pt  (iText origin = bottom-left)
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Document document = new Document(new Rectangle(842, 595), 0, 0, 0, 0)) {

            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte cb = writer.getDirectContent();

            // --- Background image ---
            try {
                Image bg = Image.getInstance(
                        getClass().getClassLoader().getResource("certificate.png"));
                bg.setAbsolutePosition(0, 0);
                bg.scaleAbsolute(842, 595);
                cb.addImage(bg);
            } catch (Exception e) {
                log.warn("certificate.png not found in classpath, using plain background");
                // Fallback: cream background
                cb.setColorFill(new Color(254, 252, 245));
                cb.rectangle(0, 0, 842, 595);
                cb.fill();
            }

            // Helper: centre-aligned text at absolute Y (iText bottom-up)
            // The template's name slot is visually ~58% from the top → y ≈ 595*0.42 = 250 pt from bottom
            // Calibrated by measuring the template image zones:
            //   Title "CERTIFICATE OF COMPLETION"  → y ≈ 435
            //   "THIS IS TO CERTIFY THAT"          → y ≈ 330
            //   Name (script)                      → y ≈ 265
            //   "HAS SUCCESSFULLY COMPLETED THE"   → y ≈ 215
            //   Course name                        → y ≈ 185
            //   Date / score line                  → y ≈ 152
            //   Instructor (right)                 → y ≈  98

            Font nameFont  = FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 38, new Color(160, 110, 15));
            Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  16, new Color(40,  40,  40));
            Font detailFont = FontFactory.getFont(FontFactory.HELVETICA,       10, new Color(70,  70,  70));
            Font instrFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,11, new Color(55,  55,  55));

            // Student name — most prominent element
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    cb, Element.ALIGN_CENTER,
                    new com.lowagie.text.Phrase(studentName, nameFont),
                    421, 265, 0);

            // Course name
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    cb, Element.ALIGN_CENTER,
                    new com.lowagie.text.Phrase(courseName.toUpperCase(), courseFont),
                    421, 185, 0);

            // Date + score line
            String detail = "Completado con distinción el " + DATE_FMT.format(Instant.now())
                           + "   ·   Puntaje: " + score + "%";
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    cb, Element.ALIGN_CENTER,
                    new com.lowagie.text.Phrase(detail, detailFont),
                    421, 152, 0);

            // Instructor — right-aligned, bottom section
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    cb, Element.ALIGN_CENTER,
                    new com.lowagie.text.Phrase(instructorName + "\n(Instructor del Curso)", instrFont),
                    630, 98, 0);

            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF del certificado", e);
        }
    }

    public static class S3UnavailableException extends RuntimeException {
        public S3UnavailableException(String message) {
            super(message);
        }
    }
}
