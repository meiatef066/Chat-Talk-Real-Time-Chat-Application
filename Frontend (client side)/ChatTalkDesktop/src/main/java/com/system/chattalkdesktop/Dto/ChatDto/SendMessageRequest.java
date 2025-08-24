package com.system.chattalkdesktop.Dto.ChatDto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private Long chatId;
    private String content;
    private Long senderId;
    private String MessageType;
    private String timestamp;
}
