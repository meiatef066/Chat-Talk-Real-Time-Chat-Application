package com.system.chattalkdesktop.event;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Event class for chat status changes
 * Used in the Observer pattern for real-time updates
 */
@Getter
@ToString
public abstract class ChatStatusEvent {
    private final Long chatId;
    private final LocalDateTime timestamp;
    private final String eventType;

    protected ChatStatusEvent(Long chatId, String eventType) {
        this.chatId = chatId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    public static class ChatActiveEvent extends ChatStatusEvent {
        public ChatActiveEvent(Long chatId) {
            super(chatId, "CHAT_ACTIVE");
        }
    }

    public static class ChatInactiveEvent extends ChatStatusEvent {
        public ChatInactiveEvent(Long chatId) {
            super(chatId, "CHAT_INACTIVE");
        }
    }

    public static class UserTypingEvent extends ChatStatusEvent {
        private final Long userId;
        private final boolean isTyping;

        public UserTypingEvent(Long chatId, Long userId, boolean isTyping) {
            super(chatId, "USER_TYPING");
            this.userId = userId;
            this.isTyping = isTyping;
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isTyping() {
            return isTyping;
        }
    }

    public static class UserOnlineEvent extends ChatStatusEvent {
        private final Long userId;
        private final boolean isOnline;

        public UserOnlineEvent(Long chatId, Long userId, boolean isOnline) {
            super(chatId, "USER_ONLINE");
            this.userId = userId;
            this.isOnline = isOnline;
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isOnline() {
            return isOnline;
        }
    }
}
