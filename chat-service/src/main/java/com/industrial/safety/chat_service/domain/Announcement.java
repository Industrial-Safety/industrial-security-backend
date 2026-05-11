package com.industrial.safety.chat_service.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    private String id;

    private String authorId;
    private String authorName;
    private String authorRole;
    private String authorAvatarUrl;

    private String title;
    private String content;

    @Indexed
    private Instant createdAt;
}
