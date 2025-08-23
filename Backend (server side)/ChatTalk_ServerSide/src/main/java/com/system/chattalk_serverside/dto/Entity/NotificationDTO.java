package com.system.chattalk_serverside.dto.Entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NotificationDTO {
    private Long id;
    private String title;
    private Long userId;
    private String message;
    private String type;
    private String readAt;
    private boolean isRead;
    private String createdAt;
}
