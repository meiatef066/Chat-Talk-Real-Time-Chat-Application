package com.system.chattalk_serverside.dto.Entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
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
