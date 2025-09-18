package com.system.chattalk_serverside.model;

import com.system.chattalk_serverside.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_chat_created", columnList = "chat_id, created_at"),
        @Index(name = "idx_messages_sender_created", columnList = "sender_id, created_at"),
        @Index(name = "idx_messages_unread", columnList = "chat_id, is_read, sender_id"),
        @Index(name = "idx_messages_type", columnList = "message_type"),
        @Index(name = "idx_messages_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private String attachmentUrl;
    @Builder.Default
    private Boolean isRead = false;
    @Builder.Default
    private Boolean isEdited = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
