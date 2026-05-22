package com.industrial.safety.chat_service.service;

import com.industrial.safety.chat_service.dto.forum.ForumPostRequest;
import com.industrial.safety.chat_service.dto.forum.ForumPostResponse;

import java.util.List;

public interface ForumService {
    List<ForumPostResponse> getPostsByCourse(String courseId);
    ForumPostResponse createPost(String courseId, ForumPostRequest request);
    ForumPostResponse addReply(String courseId, String postId, ForumPostRequest request);
}
