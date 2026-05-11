package com.industrial.safety.chat_service.service.impl;

import com.industrial.safety.chat_service.domain.Announcement;
import com.industrial.safety.chat_service.dto.announcement.AnnouncementRequest;
import com.industrial.safety.chat_service.dto.announcement.AnnouncementResponse;
import com.industrial.safety.chat_service.exception.ResourceNotFoundException;
import com.industrial.safety.chat_service.repository.AnnouncementRepository;
import com.industrial.safety.chat_service.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Override
    public List<AnnouncementResponse> getAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public AnnouncementResponse create(AnnouncementRequest req) {
        Announcement announcement = Announcement.builder()
                .authorId(req.authorId())
                .authorName(req.authorName())
                .authorRole(req.authorRole())
                .authorAvatarUrl(req.authorAvatarUrl())
                .title(req.title())
                .content(req.content())
                .createdAt(Instant.now())
                .build();
        return toResponse(announcementRepository.save(announcement));
    }

    @Override
    public void delete(String id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement", "id", id);
        }
        announcementRepository.deleteById(id);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(
                a.getId(), a.getAuthorId(), a.getAuthorName(),
                a.getAuthorRole(), a.getAuthorAvatarUrl(),
                a.getTitle(), a.getContent(), a.getCreatedAt());
    }
}
