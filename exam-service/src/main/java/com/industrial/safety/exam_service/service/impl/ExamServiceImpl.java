package com.industrial.safety.exam_service.service.impl;

import com.industrial.safety.exam_service.dto.request.CreateExamRequest;
import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.ExamResponse;
import com.industrial.safety.exam_service.dto.response.QuestionResponse;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Exam;
import com.industrial.safety.exam_service.model.Question;
import com.industrial.safety.exam_service.parser.ExamXlsxParser;
import com.industrial.safety.exam_service.repository.ExamRepository;
import com.industrial.safety.exam_service.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamXlsxParser parser;

    @Override
    public List<QuestionResponse> parseXlsx(MultipartFile file) {
        return parser.parseForPreview(file);
    }

    @Override
    @Transactional
    public ExamResponse createExam(CreateExamRequest request, List<ParsedQuestion> questions) {
        Exam exam = Exam.builder()
                .courseId(request.courseId())
                .instructorId(request.instructorId())
                .instructorName(request.instructorName())
                .title(request.title())
                .passingScore(request.passingScore())
                .xlsxS3Key(request.xlsxS3Key())
                .build();

        List<Question> entities = IntStream.range(0, questions.size())
                .mapToObj(i -> {
                    ParsedQuestion q = questions.get(i);
                    return Question.builder()
                            .exam(exam)
                            .text(q.text())
                            .optionA(q.optionA())
                            .optionB(q.optionB())
                            .optionC(q.optionC())
                            .optionD(q.optionD())
                            .correctAnswer(q.correctAnswer())
                            .orderIndex(i)
                            .build();
                }).toList();

        exam.setQuestions(entities);
        Exam saved = examRepository.save(exam);
        log.info("Exam created id={} courseId={}", saved.getId(), saved.getCourseId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamByCourseId(String courseId) {
        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ExamNotFoundException("Examen no encontrado para curso: " + courseId));
        return toResponse(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException("Examen no encontrado: " + examId));
        return toResponse(exam);
    }

    @Override
    public boolean existsByCourseId(String courseId) {
        return examRepository.existsByCourseId(courseId);
    }

    private ExamResponse toResponse(Exam exam) {
        List<QuestionResponse> qs = exam.getQuestions().stream()
                .map(q -> new QuestionResponse(
                        q.getId(), q.getText(),
                        q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                        q.getOrderIndex()))
                .toList();
        return new ExamResponse(
                exam.getId(), exam.getCourseId(),
                exam.getInstructorId(), exam.getInstructorName(),
                exam.getTitle(), exam.getPassingScore(),
                exam.getCreatedAt(), qs);
    }
}
