package com.industrial.safety.exam_service.service;

import com.industrial.safety.exam_service.dto.request.CreateExamRequest;
import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.ExamResponse;
import com.industrial.safety.exam_service.dto.response.QuestionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExamService {
    List<QuestionResponse> parseXlsx(MultipartFile file);
    ExamResponse createExam(CreateExamRequest request, List<ParsedQuestion> questions);
    ExamResponse getExamByCourseId(String courseId);
    ExamResponse getExamById(Long examId);
    boolean existsByCourseId(String courseId);
}
