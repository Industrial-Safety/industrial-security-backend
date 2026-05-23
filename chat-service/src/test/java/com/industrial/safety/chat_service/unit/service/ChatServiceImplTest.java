package com.industrial.safety.chat_service.unit.service;

import com.industrial.safety.chat_service.domain.Conversation;
import com.industrial.safety.chat_service.domain.ConversationType;
import com.industrial.safety.chat_service.domain.Message;
import com.industrial.safety.chat_service.dto.chat.ConversationRequest;
import com.industrial.safety.chat_service.dto.chat.MessageRequest;
import com.industrial.safety.chat_service.exception.ResourceNotFoundException;
import com.industrial.safety.chat_service.repository.ConversationRepository;
import com.industrial.safety.chat_service.repository.MessageRepository;
import com.industrial.safety.chat_service.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl — Pruebas Unitarias")
class DChatServiceImplTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository      messageRepository;

    @InjectMocks ChatServiceImpl chatService;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = Conversation.builder()
                .id("conv-1")
                .type(ConversationType.INSTRUCTOR)
                .studentId("student-1")
                .studentName("María López")
                .otherPartyId("instructor-1")
                .otherPartyName("Dr. García")
                .otherPartyRole("INSTRUCTOR")
                .courseId("course-1")
                .courseName("Seguridad Industrial")
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();
    }

    // =========================================================
    //  getConversationsForStudent
    // =========================================================

    @Nested
    @DisplayName("getConversationsForStudent")
    class GetConversationsForStudentTests {

        @Test
        @DisplayName("retorna lista de conversaciones del estudiante")
        void getConversationsForStudent_returnsList() {
            given(conversationRepository.findByStudentIdOrderByLastMessageAtDesc("student-1"))
                    .willReturn(List.of(conversation));

            var result = chatService.getConversationsForStudent("student-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).studentId()).isEqualTo("student-1");
            assertThat(result.get(0).otherPartyId()).isEqualTo("instructor-1");
        }

        @Test
        @DisplayName("retorna lista vacía si no hay conversaciones")
        void getConversationsForStudent_empty() {
            given(conversationRepository.findByStudentIdOrderByLastMessageAtDesc("unknown"))
                    .willReturn(List.of());

            assertThat(chatService.getConversationsForStudent("unknown")).isEmpty();
        }
    }

    // =========================================================
    //  getConversationsForInstructor
    // =========================================================

    @Test
    @DisplayName("getConversationsForInstructor: retorna conversaciones del instructor")
    void getConversationsForInstructor_returnsList() {
        given(conversationRepository.findByOtherPartyIdOrderByLastMessageAtDesc("instructor-1"))
                .willReturn(List.of(conversation));

        var result = chatService.getConversationsForInstructor("instructor-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("conv-1");
    }

    // =========================================================
    //  findOrCreateConversation
    // =========================================================

    @Nested
    @DisplayName("findOrCreateConversation")
    class FindOrCreateConversationTests {

        private ConversationRequest request;

        @BeforeEach
        void setup() {
            request = new ConversationRequest(
                    ConversationType.INSTRUCTOR, "student-1", "María López", null,
                    "instructor-1", "Dr. García", "INSTRUCTOR", null,
                    "course-1", "Seguridad Industrial"
            );
        }

        @Test
        @DisplayName("retorna conversación existente sin crear nueva")
        void findOrCreate_existingFound_returns() {
            given(conversationRepository.findByStudentIdAndOtherPartyIdAndType(
                    "student-1", "instructor-1", ConversationType.INSTRUCTOR))
                    .willReturn(Optional.of(conversation));

            var result = chatService.findOrCreateConversation(request);

            assertThat(result.id()).isEqualTo("conv-1");
            then(conversationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("crea nueva conversación cuando no existe")
        void findOrCreate_notFound_creates() {
            given(conversationRepository.findByStudentIdAndOtherPartyIdAndType(
                    "student-1", "instructor-1", ConversationType.INSTRUCTOR))
                    .willReturn(Optional.empty());
            given(conversationRepository.save(any(Conversation.class))).willReturn(conversation);

            var result = chatService.findOrCreateConversation(request);

            assertThat(result).isNotNull();
            then(conversationRepository).should().save(any(Conversation.class));
        }
    }

    // =========================================================
    //  getMessages
    // =========================================================

    @Nested
    @DisplayName("getMessages")
    class GetMessagesTests {

        @Test
        @DisplayName("retorna mensajes cuando la conversación existe")
        void getMessages_conversationFound() {
            Message msg = Message.builder()
                    .id("msg-1").conversationId("conv-1")
                    .senderId("student-1").senderName("María")
                    .content("Hola").createdAt(Instant.now()).read(false)
                    .build();

            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.findByConversationId(eq("conv-1"), any(Sort.class)))
                    .willReturn(List.of(msg));

            var result = chatService.getMessages("conv-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).senderId()).isEqualTo("student-1");
            assertThat(result.get(0).content()).isEqualTo("Hola");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la conversación no existe")
        void getMessages_conversationNotFound_throws() {
            given(conversationRepository.findById("no-conv")).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.getMessages("no-conv"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================
    //  sendMessage
    // =========================================================

    @Nested
    @DisplayName("sendMessage")
    class SendMessageTests {

        @Test
        @DisplayName("estudiante envía: incrementa unreadForOtherParty")
        void sendMessage_studentSends_incrementsUnreadForOtherParty() {
            MessageRequest req = new MessageRequest("student-1", "María", "STUDENT", null, "Hola");
            Message saved = Message.builder()
                    .id("msg-1").conversationId("conv-1").senderId("student-1")
                    .senderName("María").content("Hola").createdAt(Instant.now()).read(false).build();

            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(conversationRepository.save(any())).willReturn(conversation);

            chatService.sendMessage("conv-1", req);

            assertThat(conversation.getUnreadForOtherParty()).isEqualTo(1);
            assertThat(conversation.getUnreadForStudent()).isEqualTo(0);
        }

        @Test
        @DisplayName("instructor envía: incrementa unreadForStudent")
        void sendMessage_instructorSends_incrementsUnreadForStudent() {
            MessageRequest req = new MessageRequest("instructor-1", "Dr. García", "INSTRUCTOR", null, "Respuesta");
            Message saved = Message.builder()
                    .id("msg-2").conversationId("conv-1").senderId("instructor-1")
                    .senderName("Dr. García").content("Respuesta").createdAt(Instant.now()).read(false).build();

            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(conversationRepository.save(any())).willReturn(conversation);

            chatService.sendMessage("conv-1", req);

            assertThat(conversation.getUnreadForStudent()).isEqualTo(1);
            assertThat(conversation.getUnreadForOtherParty()).isEqualTo(0);
        }

        @Test
        @DisplayName("contenido largo se trunca a 57 caracteres + '...' en el preview")
        void sendMessage_longContent_truncatesPreview() {
            String longContent = "A".repeat(100);
            MessageRequest req = new MessageRequest("student-1", "María", "STUDENT", null, longContent);
            Message saved = Message.builder()
                    .id("msg-3").conversationId("conv-1").senderId("student-1")
                    .content(longContent).createdAt(Instant.now()).read(false).build();

            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(conversationRepository.save(any())).willReturn(conversation);

            chatService.sendMessage("conv-1", req);

            assertThat(conversation.getLastMessagePreview()).isEqualTo("A".repeat(57) + "...");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si la conversación no existe")
        void sendMessage_convNotFound_throws() {
            MessageRequest req = new MessageRequest("student-1", "María", null, null, "Hola");
            given(conversationRepository.findById("no-conv")).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.sendMessage("no-conv", req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================
    //  markAsRead
    // =========================================================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("estudiante marca como leído: resetea unreadForStudent a 0")
        void markAsRead_student_resetsUnreadForStudent() {
            conversation.setUnreadForStudent(5);
            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.findByConversationId(eq("conv-1"), any(Sort.class))).willReturn(List.of());

            chatService.markAsRead("conv-1", "student-1");

            assertThat(conversation.getUnreadForStudent()).isEqualTo(0);
            then(conversationRepository).should().save(conversation);
        }

        @Test
        @DisplayName("instructor marca como leído: resetea unreadForOtherParty a 0")
        void markAsRead_instructor_resetsUnreadForOtherParty() {
            conversation.setUnreadForOtherParty(3);
            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.findByConversationId(eq("conv-1"), any(Sort.class))).willReturn(List.of());

            chatService.markAsRead("conv-1", "instructor-1");

            assertThat(conversation.getUnreadForOtherParty()).isEqualTo(0);
        }

        @Test
        @DisplayName("marca mensajes no leídos del otro remitente como leídos")
        void markAsRead_marksMessagesFromOtherSenderAsRead() {
            Message unread = Message.builder()
                    .id("msg-1").conversationId("conv-1")
                    .senderId("instructor-1").read(false).build();

            given(conversationRepository.findById("conv-1")).willReturn(Optional.of(conversation));
            given(messageRepository.findByConversationId(eq("conv-1"), any(Sort.class)))
                    .willReturn(List.of(unread));
            given(messageRepository.save(any())).willReturn(unread);

            chatService.markAsRead("conv-1", "student-1");

            assertThat(unread.isRead()).isTrue();
            then(messageRepository).should().save(unread);
        }
    }
}
