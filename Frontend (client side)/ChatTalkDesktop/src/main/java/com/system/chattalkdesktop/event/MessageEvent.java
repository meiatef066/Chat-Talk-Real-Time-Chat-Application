package com.system.chattalkdesktop.event;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Event class for message-related operations in the frontend
 * Used in the Observer pattern for real-time updates
 */
@Getter
@ToString
public abstract class MessageEvent {
    private final MessageDTO message;
    private final LocalDateTime timestamp;
    private final String eventType;

    protected MessageEvent(MessageDTO message, String eventType) {
        this.message = message;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    public static class MessageReceivedEvent extends MessageEvent {
        public MessageReceivedEvent(MessageDTO message) {
            super(message, "MESSAGE_RECEIVED");
        }
    }

    public static class MessageSentEvent extends MessageEvent {
        public MessageSentEvent(MessageDTO message) {
            super(message, "MESSAGE_SENT");
        }
    }

    @Getter
    public static class MessageReadEvent extends MessageEvent {
        private final Long readByUserId;

        public MessageReadEvent(MessageDTO message, Long readByUserId) {
            super(message, "MESSAGE_READ");
            this.readByUserId = readByUserId;
        }

    }
}
