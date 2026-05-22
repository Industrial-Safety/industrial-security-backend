package com.industrial.safety.payment_service.pdf;

import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.dto.event.OrderItemEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptPdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final String S3_KEY_PREFIX = "receipts/";
    private static final Duration PRESIGNED_URL_TTL = Duration.ofDays(7);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Builds the PDF in memory, uploads it to S3 under receipts/{orderNumber}.pdf,
     * and returns a presigned GET URL valid for {@link #PRESIGNED_URL_TTL}.
     */
    @CircuitBreaker(name = "aws-s3", fallbackMethod = "fallbackGenerateAndUpload")
    @Retry(name = "aws-s3")
    public String generateAndUpload(Payment payment, List<OrderItemEvent> items) {
        byte[] pdfBytes = renderPdf(payment, items);
        String key = S3_KEY_PREFIX + payment.getOrderNumber() + ".pdf";

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/pdf")
                    .contentDisposition("inline; filename=\"" + payment.getOrderNumber() + ".pdf\"")
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));
            log.info("Receipt PDF uploaded to s3://{}/{} ({} bytes)",
                    bucketName, key, pdfBytes.length);
        } catch (RuntimeException ex) {
            throw new PaymentProcessingException("receipt_s3_upload_failed",
                    "Failed to upload receipt PDF to S3 for order " + payment.getOrderNumber(), ex);
        }

        return buildPresignedUrl(key);
    }

    @SuppressWarnings("unused")
    private String fallbackGenerateAndUpload(Payment payment, List<OrderItemEvent> items, Throwable ex) {
        log.error("AWS S3 circuit open — no se pudo subir recibo para orden {}: {}",
                payment.getOrderNumber(), ex.getMessage());
        throw new PaymentProcessingException("receipt_s3_unavailable",
                "Servicio de almacenamiento no disponible. El recibo no pudo generarse.", ex);
    }

    private String buildPresignedUrl(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .getObjectRequest(getRequest)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toExternalForm();
    }

    private byte[] renderPdf(Payment payment, List<OrderItemEvent> items) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Document document = new Document()) {
            PdfWriter.getInstance(document, out);
            document.open();
            writeHeader(document, payment);
            writeItemTable(document, items);
            writeFooter(document, payment);
            document.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new PaymentProcessingException("receipt_render_failed",
                    "Failed to render receipt PDF for order " + payment.getOrderNumber(), ex);
        }
    }

    private void writeHeader(Document document, Payment payment) {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph title = new Paragraph("Industrial Safety Tech - Receipt", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        document.add(new Paragraph("Order: " + payment.getOrderNumber(), labelFont));
        document.add(new Paragraph("Payment Intent: "
                + (payment.getPaymentIntentId() == null ? "-" : payment.getPaymentIntentId()), labelFont));
        document.add(new Paragraph("Customer: "
                + (payment.getUserEmail() == null ? "-" : payment.getUserEmail()), labelFont));
        Instant when = payment.getPaidAt() == null ? Instant.now() : payment.getPaidAt();
        document.add(new Paragraph("Issued at: " + DATE_FORMAT.format(when), labelFont));
        document.add(new Paragraph(" "));
    }

    private void writeItemTable(Document document, List<OrderItemEvent> items) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        addHeaderCell(table, "Course");
        addHeaderCell(table, "Course ID");
        addHeaderCell(table, "Price");

        if (items != null) {
            for (OrderItemEvent item : items) {
                table.addCell(safe(item.courseName()));
                table.addCell(safe(item.courseId()));
                table.addCell(item.price() == null ? "-" : item.price().toPlainString());
            }
        }
        document.add(table);
    }

    private void writeFooter(Document document, Payment payment) {
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Paragraph total = new Paragraph(
                "Total paid: " + payment.getAmount().setScale(2, RoundingMode.HALF_UP)
                        + " " + payment.getCurrency(),
                totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(15f);
        document.add(total);

        Paragraph thanks = new Paragraph(
                "Thank you for your purchase. Access your courses in your dashboard.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        thanks.setSpacingBefore(20f);
        thanks.setAlignment(Element.ALIGN_CENTER);
        document.add(thanks);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        cell.setBackgroundColor(new Color(33, 64, 95));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
