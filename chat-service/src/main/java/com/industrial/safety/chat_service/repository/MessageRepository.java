package com.industrial.safety.chat_service.repository;

import com.industrial.safety.chat_service.domain.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationId(String conversationId, Sort sort);
    long countByConversationIdAndReadFalseAndSenderIdNot(String conversationId, String senderId);
}
