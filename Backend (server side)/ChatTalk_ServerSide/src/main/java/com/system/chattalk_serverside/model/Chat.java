package com.system.chattalk_serverside.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "chats", indexes = {
    @Index(name = "idx_chats_type", columnList = "chat_type"),
    @Index(name = "idx_chats_created_by", columnList = "created_by"),
    @Index(name = "idx_chats_created_at", columnList = "created_at"),
    @Index(name = "idx_chats_updated_at", columnList = "updated_at"),
    @Index(name = "idx_chats_name", columnList = "name"),
    @Index(name = "idx_chats_type_updated", columnList = "chat_type, updated_at"),
    @Index(name = "idx_chats_created_by_updated", columnList = "created_by, updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private com.system.chattalk_serverside.enums.ChatType chatType = com.system.chattalk_serverside.enums.ChatType.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private String description;
    private String avatarUrl;

    private String lastMessage;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ChatParticipation> participants = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
