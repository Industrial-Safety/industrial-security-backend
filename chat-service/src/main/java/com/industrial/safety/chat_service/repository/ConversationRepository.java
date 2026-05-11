package com.industrial.safety.chat_service.repository;

import com.industrial.safety.chat_service.domain.Conversation;
import com.industrial.safety.chat_service.domain.ConversationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    List<Conversation> findByStudentIdOrderByLastMessageAtDesc(String studentId);

    List<Conversation> findByOtherPartyIdOrderByLastMessageAtDesc(String otherPartyId);

    Optional<Conversation> findByStudentIdAndOtherPartyIdAndType(
            String studentId, String otherPartyId, ConversationType type);
}
