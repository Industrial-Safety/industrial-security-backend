package com.industrial.safety.chat_service.service.impl;

import com.industrial.safety.chat_service.domain.Conversation;
import com.industrial.safety.chat_service.domain.Message;
import com.industrial.safety.chat_service.dto.chat.ConversationRequest;
import com.industrial.safety.chat_service.dto.chat.ConversationResponse;
import com.industrial.safety.chat_service.dto.chat.MessageRequest;
import com.industrial.safety.chat_service.dto.chat.MessageResponse;
import com.industrial.safety.chat_service.exception.ResourceNotFoundException;
import com.industrial.safety.chat_service.repository.ConversationRepository;
import com.industrial.safety.chat_service.repository.MessageRepository;
import com.industrial.safety.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public List<ConversationResponse> getConversationsForStudent(String studentId) {
        return conversationRepository.findByStudentIdOrderByLastMessageAtDesc(studentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<ConversationResponse> getConversationsForInstructor(String instructorId) {
        return conversationRepository.findByOtherPartyIdOrderByLastMessageAtDesc(instructorId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ConversationResponse findOrCreateConversation(ConversationRequest req) {
        return conversationRepository
                .findByStudentIdAndOtherPartyIdAndType(req.studentId(), req.otherPartyId(), req.type())
                .map(this::toResponse)
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .type(req.type())
                            .studentId(req.studentId())
                            .studentName(req.studentName())
                            .studentAvatarUrl(req.studentAvatarUrl())
                            .otherPartyId(req.otherPartyId())
                            .otherPartyName(req.otherPartyName())
                            .otherPartyRole(req.otherPartyRole())
                            .otherPartyAvatarUrl(req.otherPartyAvatarUrl())
                            .courseId(req.courseId())
                            .courseName(req.courseName())
                            .createdAt(Instant.now())
                            .lastMessageAt(Instant.now())
                            .build();
                    return toResponse(conversationRepository.save(conv));
                });
    }

    @Override
    public List<MessageResponse> getMessages(String conversationId) {
        ensureConversationExists(conversationId);
        return messageRepository
                .findByConversationId(conversationId, Sort.by(Sort.Direction.ASC, "createdAt"))
                .stream().map(this::toMessageResponse).toList();
    }

    @Override
    public MessageResponse sendMessage(String conversationId, MessageRequest req) {
        Conversation conv = ensureConversationExists(conversationId);

        Message msg = Message.builder()
                .conversationId(conversationId)
                .senderId(req.senderId())
                .senderName(req.senderName())
                .senderRole(req.senderRole())
                .senderAvatarUrl(req.senderAvatarUrl())
                .content(req.content())
                .createdAt(Instant.now())
                .read(false)
                .build();

        Message saved = messageRepository.save(msg);

        // Actualizar preview y contadores de no leídos
        boolean senderIsStudent = req.senderId().equals(conv.getStudentId());
        conv.setLastMessageAt(saved.getCreatedAt());
        conv.setLastMessagePreview(req.content().length() > 60
                ? req.content().substring(0, 57) + "..."
                : req.content());
        if (senderIsStudent) {
            conv.setUnreadForOtherParty(conv.getUnreadForOtherParty() + 1);
        } else {
            conv.setUnreadForStudent(conv.getUnreadForStudent() + 1);
        }
        conversationRepository.save(conv);

        return toMessageResponse(saved);
    }

    @Override
    public void markAsRead(String conversationId, String readerId) {
        Conversation conv = ensureConversationExists(conversationId);
        boolean isStudent = readerId.equals(conv.getStudentId());
        if (isStudent) {
            conv.setUnreadForStudent(0);
        } else {
            conv.setUnreadForOtherParty(0);
        }
        conversationRepository.save(conv);

        // Marcar mensajes como leídos
        messageRepository.findByConversationId(conversationId, Sort.by("createdAt"))
                .stream()
                .filter(m -> !m.getSenderId().equals(readerId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private Conversation ensureConversationExists(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
    }

    private ConversationResponse toResponse(Conversation c) {
        return new ConversationResponse(
                c.getId(), c.getType(),
                c.getStudentId(), c.getStudentName(), c.getStudentAvatarUrl(),
                c.getOtherPartyId(), c.getOtherPartyName(), c.getOtherPartyRole(), c.getOtherPartyAvatarUrl(),
                c.getCourseId(), c.getCourseName(),
                c.getCreatedAt(), c.getLastMessageAt(), c.getLastMessagePreview(),
                c.getUnreadForStudent(), c.getUnreadForOtherParty());
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
                m.getId(), m.getConversationId(),
                m.getSenderId(), m.getSenderName(), m.getSenderRole(), m.getSenderAvatarUrl(),
                m.getContent(), m.getCreatedAt(), m.isRead());
    }
}
