package com.system.chattalk_serverside.controller.ChatController;

import com.system.chattalk_serverside.dto.ChatDto.SendMessageRequest;
import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import com.system.chattalk_serverside.service.Message.MessageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class RealtimeMessaging {
    private final MessageServiceImpl messageServiceImpl;

    public RealtimeMessaging( MessageServiceImpl messageServiceImpl ) {
        this.messageServiceImpl = messageServiceImpl;
    }

    /**
     * Handle ping messages for connection testing
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public String handlePing() {
        log.debug("Ping received, sending pong response");
        return "Pong: WebSocket connection is active";
    }

    @MessageMapping("/chat.privateMessage")
    public MessageDTO sendMessage( SendMessageRequest request) {
        log.info("Message sent to Messaging service {}", request.getSenderId());
        return messageServiceImpl.sendMessage(request);
    }
}
