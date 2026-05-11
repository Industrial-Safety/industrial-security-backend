package com.industrial.safety.course_service.service;

import com.industrial.safety.course_service.dto.PriceChangeRequestDto;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;

import java.util.List;

public interface PriceChangeRequestService {
    PriceChangeRequestDto.Response create(PriceChangeRequestDto.CreateRequest req);
    List<PriceChangeRequestDto.Response> getAll();
    List<PriceChangeRequestDto.Response> getPending();
    List<PriceChangeRequestDto.Response> getByRequester(String requesterId);
    PriceChangeRequestDto.Response review(String id, PriceChangeRequestDto.ReviewRequest req);
}
