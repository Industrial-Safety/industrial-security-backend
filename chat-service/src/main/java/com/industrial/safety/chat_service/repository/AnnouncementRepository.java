package com.industrial.safety.chat_service.repository;

import com.industrial.safety.chat_service.domain.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
