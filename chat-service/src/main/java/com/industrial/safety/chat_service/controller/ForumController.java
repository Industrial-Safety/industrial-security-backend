package com.industrial.safety.chat_service.controller;

import com.industrial.safety.chat_service.dto.forum.ForumPostRequest;
import com.industrial.safety.chat_service.dto.forum.ForumPostResponse;
import com.industrial.safety.chat_service.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/{courseId}")
    public List<ForumPostResponse> getPostsByCourse(@PathVariable String courseId) {
        return forumService.getPostsByCourse(courseId);
    }

    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ForumPostResponse createPost(@PathVariable String courseId,
                                        @Valid @RequestBody ForumPostRequest request) {
        return forumService.createPost(courseId, request);
    }

    @PostMapping("/{courseId}/{postId}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    public ForumPostResponse addReply(@PathVariable String courseId,
                                      @PathVariable String postId,
                                      @Valid @RequestBody ForumPostRequest request) {
        return forumService.addReply(courseId, postId, request);
    }
}
