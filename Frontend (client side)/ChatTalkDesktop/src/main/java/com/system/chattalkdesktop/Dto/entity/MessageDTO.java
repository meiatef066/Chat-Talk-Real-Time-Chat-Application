package com.system.chattalkdesktop.Dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long messageId;
    private Long chatId;
    private String chatName;
    private String messageType;
    private LocalDateTime timestamp;
    private Long senderId;
    private String content;
    private Boolean isRead;
}