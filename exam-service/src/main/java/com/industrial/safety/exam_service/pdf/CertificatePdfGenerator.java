package com.industrial.safety.exam_service.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificatePdfGenerator {

    private static final String S3_KEY_PREFIX = "certificates/";
    private static final Duration PRESIGN_TTL = Duration.ofDays(7);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd 'de' MMMM 'de' yyyy")
            .withZone(ZoneId.systemDefault())
            .withLocale(new Locale("es", "ES"));

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @CircuitBreaker(name = "aws-s3", fallbackMethod = "fallbackGenerateAndUpload")
    @Retry(name = "aws-s3")
    public String generateAndUpload(String studentId, Long examId,
                                    String studentName, String courseName,
                                    String instructorName, Integer score) {
        byte[] pdf = render(studentId, examId, studentName, courseName, instructorName, score);
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

    private byte[] render(String studentId, Long examId,
                          String studentName, String courseName,
                          String instructorName, Integer score) {
        try {
            String html = loadTemplate()
                    .replace("{{STUDENT_NAME}}",      escapeHtml(studentName))
                    .replace("{{COURSE_NAME}}",        escapeHtml(courseName))
                    .replace("{{INSTRUCTOR_NAME}}",    escapeHtml(instructorName))
                    .replace("{{SCORE}}",              score + "%")
                    .replace("{{ISSUE_DATE}}",         DATE_FMT.format(Instant.now()))
                    .replace("{{VERIFICATION_CODE}}", buildCode(studentId, examId));

            ClassLoader cl = getClass().getClassLoader();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.useSVGDrawer(new BatikSVGDrawer());

                // Fuentes Fraunces (serif — nombre del alumno y curso)
                builder.useFont(() -> cl.getResourceAsStream("fonts/Fraunces-Regular.ttf"),  "Fraunces", 400, FontStyle.NORMAL, true);
                builder.useFont(() -> cl.getResourceAsStream("fonts/Fraunces-SemiBold.ttf"), "Fraunces", 600, FontStyle.NORMAL, true);
                builder.useFont(() -> cl.getResourceAsStream("fonts/Fraunces-Bold.ttf"),     "Fraunces", 700, FontStyle.NORMAL, true);

                // Fuentes Sora (sans-serif — todo el texto de cuerpo y etiquetas)
                builder.useFont(() -> cl.getResourceAsStream("fonts/Sora-Regular.ttf"),     "Sora", 400, FontStyle.NORMAL, true);
                builder.useFont(() -> cl.getResourceAsStream("fonts/Sora-SemiBold.ttf"),    "Sora", 600, FontStyle.NORMAL, true);
                builder.useFont(() -> cl.getResourceAsStream("fonts/Sora-Bold.ttf"),        "Sora", 700, FontStyle.NORMAL, true);

                builder.withHtmlContent(html, null);
                builder.toStream(out);
                builder.run();
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF del certificado", e);
        }
    }

    private String loadTemplate() throws IOException {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("templates/certificate.html")) {
            if (is == null) {
                throw new IllegalStateException("templates/certificate.html no encontrado en el classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String buildCode(String studentId, Long examId) {
        String suffix = studentId.length() >= 4
                ? studentId.substring(0, 4).toUpperCase()
                : studentId.toUpperCase();
        return "ISA-" + String.format("%04d", examId) + "-" + suffix;
    }

    public static class S3UnavailableException extends RuntimeException {
        public S3UnavailableException(String message) {
            super(message);
        }
    }
}
