package com.industrial.safety.chat_service.controller;

import com.industrial.safety.chat_service.dto.chat.ConversationRequest;
import com.industrial.safety.chat_service.dto.chat.ConversationResponse;
import com.industrial.safety.chat_service.dto.chat.MessageRequest;
import com.industrial.safety.chat_service.dto.chat.MessageResponse;
import com.industrial.safety.chat_service.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/student/{studentId}")
    public List<ConversationResponse> getConversationsForStudent(@PathVariable String studentId) {
        return chatService.getConversationsForStudent(studentId);
    }

    @GetMapping("/instructor/{instructorId}")
    public List<ConversationResponse> getConversationsForInstructor(@PathVariable String instructorId) {
        return chatService.getConversationsForInstructor(instructorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ConversationResponse findOrCreateConversation(@Valid @RequestBody ConversationRequest request) {
        return chatService.findOrCreateConversation(request);
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(@PathVariable String conversationId) {
        return chatService.getMessages(conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable String conversationId,
                                       @Valid @RequestBody MessageRequest request) {
        return chatService.sendMessage(conversationId, request);
    }

    @PatchMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable String conversationId,
                           @RequestParam String readerId) {
        chatService.markAsRead(conversationId, readerId);
    }
}
