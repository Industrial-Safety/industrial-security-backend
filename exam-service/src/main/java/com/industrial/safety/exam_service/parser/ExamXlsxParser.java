package com.industrial.safety.exam_service.parser;

import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.QuestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ExamXlsxParser {

    /**
     * Columns: Pregunta | OpcionA | OpcionB | OpcionC | OpcionD | Correcta (A/B/C/D)
     * First row is header (skipped).
     */
    public List<ParsedQuestion> parse(MultipartFile file) {
        List<ParsedQuestion> questions = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int order = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String text    = cell(row, 0);
                String a       = cell(row, 1);
                String b       = cell(row, 2);
                String c       = cell(row, 3);
                String d       = cell(row, 4);
                String correct = cell(row, 5).toUpperCase();
                if (text.isBlank() || a.isBlank() || correct.isBlank()) continue;
                questions.add(new ParsedQuestion(text, a, b, c, d, correct, order++));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo xlsx: " + e.getMessage());
        }
        log.info("Parsed {} questions from xlsx", questions.size());
        return questions;
    }

    /** Preview for instructor (no correctAnswer exposed) */
    public List<QuestionResponse> parseForPreview(MultipartFile file) {
        return parse(file).stream()
                .map(q -> new QuestionResponse(null, q.text(), q.optionA(),
                        q.optionB(), q.optionC(), q.optionD(), q.orderIndex()))
                .toList();
    }

    private String cell(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}
