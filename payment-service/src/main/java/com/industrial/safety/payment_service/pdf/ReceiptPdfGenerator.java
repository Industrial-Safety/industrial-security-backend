package com.industrial.safety.payment_service.pdf;

import com.industrial.safety.payment_service.config.properties.ReceiptProperties;
import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.dto.event.OrderItemEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
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
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final ReceiptProperties properties;

    public Path generate(Payment payment, List<OrderItemEvent> items) {
        try {
            Path outputDir = Path.of(properties.storage().outputDir()).toAbsolutePath();
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve(payment.getOrderNumber() + ".pdf");

            try (OutputStream os = Files.newOutputStream(file);
                 Document document = new Document()) {
                PdfWriter.getInstance(document, os);
                document.open();
                writeHeader(document, payment);
                writeItemTable(document, items);
                writeFooter(document, payment);
            }
            log.info("Receipt PDF generated for orderNumber={} at {}", payment.getOrderNumber(), file);
            return file;
        } catch (IOException ex) {
            throw new PaymentProcessingException("receipt_io_error",
                    "Failed to write receipt PDF for order " + payment.getOrderNumber(), ex);
        }
    }

    public String buildPublicUrl(String orderNumber) {
        String base = properties.storage().publicBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + orderNumber + ".pdf";
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
