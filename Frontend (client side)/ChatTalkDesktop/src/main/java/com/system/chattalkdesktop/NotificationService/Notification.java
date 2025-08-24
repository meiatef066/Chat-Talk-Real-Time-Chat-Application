package com.system.chattalkdesktop.NotificationService;

import com.system.chattalkdesktop.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class Notification {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean read;
    private LocalDateTime createdAt;

    // Default constructor
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    // Constructor with title, message, and type
    public Notification(String title, String message, NotificationType type) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
    }

    // Full constructor
    public Notification(Long id, String title, String message, NotificationType type, Boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read != null ? read : false;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
