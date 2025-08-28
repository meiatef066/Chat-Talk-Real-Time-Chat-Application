package com.system.chattalk_serverside.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "friend_requests",
        indexes = {
                @Index(name = "idx_friendreq_sender_receiver", columnList = "sender_id,receiver_id"),
                @Index(name = "idx_friendreq_status", columnList = "status"),
                @Index(name = "idx_friendreq_created_at", columnList = "created_at"),
                @Index(name = "idx_friendreq_receiver_status_created", columnList = "receiver_id, status, created_at"),
                @Index(name = "idx_friendreq_sender_status_created", columnList = "sender_id, status, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;


    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED, CANCELLED
    }
}
