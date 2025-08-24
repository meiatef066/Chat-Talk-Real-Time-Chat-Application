package com.system.chattalkdesktop.Dto.ChatDto;

import lombok.*;

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
