package com.system.chattalk_serverside.dto.Entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class ChatDto {
    private Long chatId;
    private String chatName;
    private String chatType;
    private List<String> participationEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
