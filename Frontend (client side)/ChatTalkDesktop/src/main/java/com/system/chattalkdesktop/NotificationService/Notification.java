package com.system.chattalkdesktop.NotificationService;

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

}
