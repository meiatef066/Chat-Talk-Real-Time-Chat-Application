package com.system.chattalkdesktop.Dto.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private Long userId;
    private String senderEmail;
    private String message;
    private String type;
    private String readAt;
    private boolean isRead;
    private String createdAt;
    private Long chatId;
    private Long senderId;
}
