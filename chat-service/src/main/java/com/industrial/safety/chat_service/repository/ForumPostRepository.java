package com.industrial.safety.chat_service.repository;

import com.industrial.safety.chat_service.domain.ForumPost;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ForumPostRepository extends MongoRepository<ForumPost, String> {
    List<ForumPost> findByCourseIdOrderByCreatedAtDesc(String courseId);
}
