package com.industrial.safety.chat_service.service;

import com.industrial.safety.chat_service.dto.announcement.AnnouncementRequest;
import com.industrial.safety.chat_service.dto.announcement.AnnouncementResponse;

import java.util.List;

public interface AnnouncementService {
    List<AnnouncementResponse> getAll();
    AnnouncementResponse create(AnnouncementRequest request);
    void delete(String id);
}
