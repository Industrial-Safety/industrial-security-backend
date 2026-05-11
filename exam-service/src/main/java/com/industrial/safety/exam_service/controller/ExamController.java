package com.industrial.safety.exam_service.controller;

import com.industrial.safety.exam_service.dto.request.CreateExamRequest;
import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.ExamResponse;
import com.industrial.safety.exam_service.dto.response.QuestionResponse;
import com.industrial.safety.exam_service.parser.ExamXlsxParser;
import com.industrial.safety.exam_service.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final ExamXlsxParser xlsxParser;

    /** Instructor: parse xlsx — returns preview questions (no correctAnswer) */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<QuestionResponse>> parseXlsx(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(examService.parseXlsx(file));
    }

    /** Instructor: save exam with parsed questions (includes correctAnswer server-side) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExamResponse> createExam(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute CreateExamRequest request) {
        // Re-parse with correctAnswer for persistence
        List<ParsedQuestion> parsed = parseInternal(file);
        return ResponseEntity.ok(examService.createExam(request, parsed));
    }

    /** Student: get exam questions (no correctAnswer exposed) */
    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<ExamResponse> getExamByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(examService.getExamByCourseId(courseId));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.getExamById(examId));
    }

    @GetMapping("/exists/{courseId}")
    public ResponseEntity<Map<String, Boolean>> exists(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("exists", examService.existsByCourseId(courseId)));
    }

    private List<ParsedQuestion> parseInternal(MultipartFile file) {
        return xlsxParser.parse(file);
    }
}
