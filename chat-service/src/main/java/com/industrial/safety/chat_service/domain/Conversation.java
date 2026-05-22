package com.industrial.safety.chat_service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "conversations")
@CompoundIndexes({
    @CompoundIndex(name = "idx_conv_student_type", def = "{'studentId': 1, 'type': 1}"),
    @CompoundIndex(name = "idx_conv_parties", def = "{'studentId': 1, 'otherPartyId': 1, 'type': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    private ConversationType type;

    // Siempre el alumno
    private String studentId;
    private String studentName;
    private String studentAvatarUrl;

    // Instructor o Soporte según el tipo
    private String otherPartyId;
    private String otherPartyName;
    private String otherPartyRole;
    private String otherPartyAvatarUrl;

    // Solo para INSTRUCTOR — identifica el curso del que hablan
    private String courseId;
    private String courseName;

    private Instant createdAt;
    private Instant lastMessageAt;
    private String lastMessagePreview;

    @Builder.Default
    private int unreadForStudent = 0;

    @Builder.Default
    private int unreadForOtherParty = 0;
}
