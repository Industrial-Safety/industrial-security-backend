package com.industrial.safety.chat_service.service.impl;

import com.industrial.safety.chat_service.domain.ForumPost;
import com.industrial.safety.chat_service.dto.forum.ForumPostRequest;
import com.industrial.safety.chat_service.dto.forum.ForumPostResponse;
import com.industrial.safety.chat_service.exception.ResourceNotFoundException;
import com.industrial.safety.chat_service.repository.ForumPostRepository;
import com.industrial.safety.chat_service.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private final ForumPostRepository forumPostRepository;

    @Override
    public List<ForumPostResponse> getPostsByCourse(String courseId) {
        return forumPostRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ForumPostResponse createPost(String courseId, ForumPostRequest request) {
        ForumPost post = ForumPost.builder()
                .courseId(courseId)
                .authorId(request.authorId())
                .authorName(request.authorName())
                .authorRole(request.authorRole())
                .authorAvatarUrl(request.authorAvatarUrl())
                .content(request.content())
                .createdAt(Instant.now())
                .build();
        return toResponse(forumPostRepository.save(post));
    }

    @Override
    public ForumPostResponse addReply(String courseId, String postId, ForumPostRequest request) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("ForumPost", "id", postId));

        ForumPost.ForumReply reply = ForumPost.ForumReply.builder()
                .authorId(request.authorId())
                .authorName(request.authorName())
                .authorRole(request.authorRole())
                .authorAvatarUrl(request.authorAvatarUrl())
                .content(request.content())
                .createdAt(Instant.now())
                .build();

        post.getReplies().add(reply);
        return toResponse(forumPostRepository.save(post));
    }

    private ForumPostResponse toResponse(ForumPost post) {
        List<ForumPostResponse.ReplyResponse> replies = post.getReplies().stream()
                .map(r -> new ForumPostResponse.ReplyResponse(
                        r.getAuthorId(), r.getAuthorName(), r.getAuthorRole(),
                        r.getAuthorAvatarUrl(), r.getContent(), r.getCreatedAt()))
                .toList();

        return new ForumPostResponse(
                post.getId(), post.getCourseId(),
                post.getAuthorId(), post.getAuthorName(), post.getAuthorRole(),
                post.getAuthorAvatarUrl(), post.getContent(), post.getCreatedAt(), replies);
    }
}
