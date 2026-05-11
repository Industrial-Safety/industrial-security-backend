package com.industrial.safety.exam_service.service;

import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;

public interface AttemptService {
    AttemptResultResponse submit(Long examId, SubmitAttemptRequest request);
}
