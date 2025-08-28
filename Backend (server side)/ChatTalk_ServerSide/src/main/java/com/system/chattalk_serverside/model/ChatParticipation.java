package com.system.chattalk_serverside.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participations", indexes = {
    @Index(name = "idx_chat_participations_user", columnList = "user_id"),
    @Index(name = "idx_chat_participations_chat", columnList = "chat_id"),
    @Index(name = "idx_chat_participations_user_chat", columnList = "user_id, chat_id"),
    @Index(name = "idx_chat_participations_status", columnList = "status"),
    @Index(name = "idx_chat_participations_role", columnList = "role"),
    @Index(name = "idx_chat_participations_joined_at", columnList = "joined_at"),
    @Index(name = "idx_chat_participations_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ParticipationRole role = ParticipationRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    public enum ParticipationRole {
        ADMIN, MODERATOR, MEMBER
    }

    public enum ParticipationStatus {
        ACTIVE, MUTED, BANNED, LEFT
    }
}
