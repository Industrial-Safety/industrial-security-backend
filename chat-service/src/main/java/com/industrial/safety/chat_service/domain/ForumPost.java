package com.industrial.safety.chat_service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "forum_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPost {

    @Id
    private String id;

    @Indexed
    private String courseId;

    private String authorId;
    private String authorName;
    private String authorRole;
    private String authorAvatarUrl;
    private String content;
    private Instant createdAt;

    @Builder.Default
    private List<ForumReply> replies = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForumReply {
        private String authorId;
        private String authorName;
        private String authorRole;
        private String authorAvatarUrl;
        private String content;
        private Instant createdAt;
    }
}
