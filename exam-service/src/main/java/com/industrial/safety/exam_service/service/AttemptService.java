package com.industrial.safety.exam_service.service;

import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.dto.response.StudentAttemptSummaryResponse;

import java.util.List;

public interface AttemptService {
    AttemptResultResponse submit(Long examId, SubmitAttemptRequest request);
    List<StudentAttemptSummaryResponse> getAttemptsByExam(Long examId);
}
