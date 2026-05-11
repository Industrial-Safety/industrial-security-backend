package com.industrial.safety.chat_service.service;

import com.industrial.safety.chat_service.dto.chat.ConversationRequest;
import com.industrial.safety.chat_service.dto.chat.ConversationResponse;
import com.industrial.safety.chat_service.dto.chat.MessageRequest;
import com.industrial.safety.chat_service.dto.chat.MessageResponse;

import java.util.List;

public interface ChatService {
    List<ConversationResponse> getConversationsForStudent(String studentId);
    List<ConversationResponse> getConversationsForInstructor(String instructorId);
    ConversationResponse findOrCreateConversation(ConversationRequest request);
    List<MessageResponse> getMessages(String conversationId);
    MessageResponse sendMessage(String conversationId, MessageRequest request);
    void markAsRead(String conversationId, String readerId);
}
