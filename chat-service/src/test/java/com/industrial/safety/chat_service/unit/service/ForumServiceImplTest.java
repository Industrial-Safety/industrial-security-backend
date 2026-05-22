package com.industrial.safety.chat_service.unit.service;

import com.industrial.safety.chat_service.domain.ForumPost;
import com.industrial.safety.chat_service.dto.forum.ForumPostRequest;
import com.industrial.safety.chat_service.exception.ResourceNotFoundException;
import com.industrial.safety.chat_service.repository.ForumPostRepository;
import com.industrial.safety.chat_service.service.impl.ForumServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForumServiceImpl — Pruebas Unitarias")
class ForumServiceImplTest {

    @Mock ForumPostRepository forumPostRepository;

    @InjectMocks ForumServiceImpl forumService;

    private ForumPost post;
    private ForumPostRequest request;

    @BeforeEach
    void setUp() {
        post = ForumPost.builder()
                .id("post-1")
                .courseId("course-1")
                .authorId("student-1")
                .authorName("María López")
                .authorRole("STUDENT")
                .content("¿Cómo se usa el EPP correctamente?")
                .createdAt(Instant.now())
                .replies(new ArrayList<>())
                .build();

        request = new ForumPostRequest(
                "student-1", "María López", "STUDENT", null,
                "¿Cómo se usa el EPP correctamente?"
        );
    }

    // =========================================================
    //  getPostsByCourse
    // =========================================================

    @Test
    @DisplayName("getPostsByCourse: retorna lista de publicaciones del curso")
    void getPostsByCourse_returnsList() {
        given(forumPostRepository.findByCourseIdOrderByCreatedAtDesc("course-1")).willReturn(List.of(post));

        var result = forumService.getPostsByCourse("course-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseId()).isEqualTo("course-1");
        assertThat(result.get(0).authorId()).isEqualTo("student-1");
    }

    @Test
    @DisplayName("getPostsByCourse: retorna vacío si no hay publicaciones")
    void getPostsByCourse_empty() {
        given(forumPostRepository.findByCourseIdOrderByCreatedAtDesc("course-x")).willReturn(List.of());

        assertThat(forumService.getPostsByCourse("course-x")).isEmpty();
    }

    // =========================================================
    //  createPost
    // =========================================================

    @Test
    @DisplayName("createPost: crea y guarda la publicación en el foro")
    void createPost_savesAndReturns() {
        given(forumPostRepository.save(any(ForumPost.class))).willReturn(post);

        var result = forumService.createPost("course-1", request);

        assertThat(result.id()).isEqualTo("post-1");
        assertThat(result.content()).isEqualTo("¿Cómo se usa el EPP correctamente?");
        assertThat(result.replies()).isEmpty();
        then(forumPostRepository).should().save(any(ForumPost.class));
    }

    // =========================================================
    //  addReply
    // =========================================================

    @Test
    @DisplayName("addReply: agrega respuesta cuando la publicación existe")
    void addReply_found_addsReply() {
        given(forumPostRepository.findById("post-1")).willReturn(Optional.of(post));
        given(forumPostRepository.save(post)).willReturn(post);

        var replyRequest = new ForumPostRequest(
                "instructor-1", "Dr. García", "INSTRUCTOR", null, "Muy buena pregunta"
        );

        forumService.addReply("course-1", "post-1", replyRequest);

        assertThat(post.getReplies()).hasSize(1);
        assertThat(post.getReplies().get(0).getContent()).isEqualTo("Muy buena pregunta");
        assertThat(post.getReplies().get(0).getAuthorId()).isEqualTo("instructor-1");
        then(forumPostRepository).should().save(post);
    }

    @Test
    @DisplayName("addReply: lanza ResourceNotFoundException si la publicación no existe")
    void addReply_notFound_throws() {
        given(forumPostRepository.findById("no-post")).willReturn(Optional.empty());

        assertThatThrownBy(() -> forumService.addReply("course-1", "no-post", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
