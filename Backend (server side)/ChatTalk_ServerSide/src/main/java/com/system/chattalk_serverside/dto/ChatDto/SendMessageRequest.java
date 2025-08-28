package com.system.chattalk_serverside.dto.ChatDto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SendMessageRequest {
    private Long chatId;
    private String content;
    private Long senderId;
    private String MessageType;
    private String timestamp;
}
