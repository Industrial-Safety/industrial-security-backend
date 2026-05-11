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
    private static final Duration PRESIGN_TTL = Duration.ofDays(365);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd 'de' MMMM 'de' yyyy")
            .withZone(ZoneId.systemDefault())
            .withLocale(new java.util.Locale("es", "ES"));

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

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
        return presign(key);
    }

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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Document document = new Document(new Rectangle(842, 595))) { // A4 landscape

            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte canvas = writer.getDirectContent();

            // Background image (certificate.png from classpath)
            try {
                Image bg = Image.getInstance(
                        getClass().getClassLoader().getResource("certificate.png"));
                bg.setAbsolutePosition(0, 0);
                bg.scaleToFit(842, 595);
                canvas.addImage(bg);
            } catch (Exception e) {
                log.warn("certificate.png not found in classpath, generating plain PDF");
            }

            // Student name — large, centered, gold color
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, new Color(180, 140, 20));
            Paragraph name = new Paragraph(studentName, nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(195f);
            document.add(name);

            // Course name
            Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(50, 50, 50));
            Paragraph course = new Paragraph(courseName.toUpperCase(), courseFont);
            course.setAlignment(Element.ALIGN_CENTER);
            course.setSpacingBefore(10f);
            document.add(course);

            // Date and score
            Font detailFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(80, 80, 80));
            Paragraph detail = new Paragraph(
                    "Completado con distinción el " + DATE_FMT.format(Instant.now())
                    + "  ·  Puntaje: " + score + "%", detailFont);
            detail.setAlignment(Element.ALIGN_CENTER);
            detail.setSpacingBefore(8f);
            document.add(detail);

            // Instructor name — bottom right
            Font instrFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, new Color(60, 60, 60));
            Paragraph instr = new Paragraph(instructorName + "\n(Instructor del Curso)", instrFont);
            instr.setAlignment(Element.ALIGN_RIGHT);
            instr.setSpacingBefore(60f);
            instr.setIndentationRight(80f);
            document.add(instr);

            // Website
            Font webFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(130, 130, 130));
            Paragraph web = new Paragraph("www.industrialsafetytech.com", webFont);
            web.setAlignment(Element.ALIGN_CENTER);
            web.setSpacingBefore(4f);
            document.add(web);

            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF del certificado", e);
        }
    }
}
