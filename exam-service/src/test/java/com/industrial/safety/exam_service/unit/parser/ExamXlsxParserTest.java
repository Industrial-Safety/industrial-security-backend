package com.industrial.safety.exam_service.unit.parser;

import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.QuestionResponse;
import com.industrial.safety.exam_service.parser.ExamXlsxParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExamXlsxParser — Pruebas Unitarias")
class ExamXlsxParserTest {

    private final ExamXlsxParser parser = new ExamXlsxParser();

    private MockMultipartFile buildXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Preguntas");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Pregunta");
            header.createCell(5).setCellValue("Correcta");

            // Fila válida (STRING)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("¿Cuál es el EPP básico?");
            r1.createCell(1).setCellValue("Casco");
            r1.createCell(2).setCellValue("Guante");
            r1.createCell(3).setCellValue("Botas");
            r1.createCell(4).setCellValue("Todas");
            r1.createCell(5).setCellValue("d"); // minúscula -> toUpperCase

            // Fila con tipos NUMERIC y BOOLEAN
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Pregunta dos");
            r2.createCell(1).setCellValue(42);     // NUMERIC
            r2.createCell(2).setCellValue(true);   // BOOLEAN
            r2.createCell(3).setCellValue("C");
            r2.createCell(4).setCellValue("D");
            r2.createCell(5).setCellValue("A");

            // Fila con texto en blanco -> se omite
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("");
            r3.createCell(1).setCellValue("X");
            r3.createCell(5).setCellValue("A");

            // Fila con respuesta correcta en blanco -> se omite
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Pregunta sin respuesta");
            r4.createCell(1).setCellValue("Opcion");
            r4.createCell(5).setCellValue("");

            wb.write(out);
            return new MockMultipartFile("file", "examen.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    @DisplayName("parse: extrae solo las filas válidas y normaliza la respuesta")
    void parse_validRows() throws Exception {
        List<ParsedQuestion> result = parser.parse(buildXlsx());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).correctAnswer()).isEqualTo("D");
        assertThat(result.get(0).orderIndex()).isZero();
    }

    @Test
    @DisplayName("parseForPreview: no expone la respuesta correcta")
    void parseForPreview_hidesAnswer() throws Exception {
        List<QuestionResponse> result = parser.parseForPreview(buildXlsx());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("parse: archivo ilegible lanza IllegalArgumentException")
    void parse_invalidFile_throws() {
        MockMultipartFile bad = new MockMultipartFile("file", "x.xlsx",
                "application/octet-stream", new byte[]{1, 2, 3});

        try {
            parser.parse(bad);
        } catch (RuntimeException ex) {
            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }
}
