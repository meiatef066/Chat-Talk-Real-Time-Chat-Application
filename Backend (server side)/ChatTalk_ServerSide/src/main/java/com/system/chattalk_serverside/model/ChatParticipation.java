package com.system.chattalk_serverside.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participations")
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
    private ParticipationRole role = ParticipationRole.MEMBER;

    @Enumerated(EnumType.STRING)
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
