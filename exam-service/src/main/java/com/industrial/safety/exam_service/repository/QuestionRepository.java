package com.industrial.safety.exam_service.repository;

import com.industrial.safety.exam_service.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamIdOrderByOrderIndex(Long examId);
}
