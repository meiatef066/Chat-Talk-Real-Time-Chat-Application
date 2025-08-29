package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.event.MessageEvent;
import com.system.chattalkdesktop.event.ChatStatusEvent;
import com.system.chattalkdesktop.utils.SessionManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Chat manager service that coordinates all chat functionality
 * Integrates real-time updates, notifications, and UI management
 */
@Slf4j
public class ChatManagerService {
    private static ChatManagerService instance;
    private final RealTimeChatService realTimeService = RealTimeChatService.getInstance();
    private final RealTimeNotificationService notificationService = RealTimeNotificationService.getInstance();
    private final ChatMessageObserver messageObserver = ChatMessageObserver.getInstance();
    private final EventBus eventBus = EventBus.getInstance();
    
    // Active chat management
    private final ConcurrentHashMap<Long, ChatSession> activeChats = new ConcurrentHashMap<>();
    private Long currentActiveChatId;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    // UI update callbacks
    private Consumer<MessageDTO> messageReceivedCallback;
    private Consumer<MessageDTO> messageSentCallback;
    private Consumer<Long> chatActivatedCallback;
    private Consumer<Long> chatDeactivatedCallback;
    private Consumer<UserDTO> userStatusChangedCallback;

    private ChatManagerService() {}

    public static synchronized ChatManagerService getInstance() {
        if (instance == null) {
            instance = new ChatManagerService();
        }
        return instance;
    }

    /**
     * Initialize the chat manager
     */
    public void initialize() {
        if (isInitialized.get()) {
            log.warn("ChatManagerService already initialized");
            return;
        }

        log.info("Initializing ChatManagerService");
        
        // Setup event subscriptions
        setupEventSubscriptions();
        
        // Setup message observer
        setupMessageObserver();
        
        // Initialize notification service
        notificationService.getInstance();
        
        isInitialized.set(true);
        log.info("ChatManagerService initialized successfully");
    }

    /**
     * Setup event subscriptions
     */
    private void setupEventSubscriptions() {
        // Subscribe to message events
        eventBus.subscribe(MessageEvent.MessageReceivedEvent.class, this::handleMessageReceived);
        eventBus.subscribe(MessageEvent.MessageSentEvent.class, this::handleMessageSent);
        eventBus.subscribe(MessageEvent.MessageReadEvent.class, this::handleMessageRead);
        
        // Subscribe to chat status events
        eventBus.subscribe(ChatStatusEvent.ChatActiveEvent.class, this::handleChatActive);
        eventBus.subscribe(ChatStatusEvent.ChatInactiveEvent.class, this::handleChatInactive);
        eventBus.subscribe(ChatStatusEvent.UserTypingEvent.class, this::handleUserTyping);
        eventBus.subscribe(ChatStatusEvent.UserOnlineEvent.class, this::handleUserOnline);
    }

    /**
     * Setup message observer
     */
    private void setupMessageObserver() {
        messageObserver.addListener(new MessageUpdateListener() {
            @Override
            public void onNewMessage(MessageDTO message) {
                handleMessageReceived(new MessageEvent.MessageReceivedEvent(message));
            }

            @Override
            public void onMessageRead(Long chatId, Long userId) {
                handleMessageRead(new MessageEvent.MessageReadEvent(
                    new MessageDTO(), userId
                ));
            }

            @Override
            public void onMessageSent(MessageDTO message) {
                handleMessageSent(new MessageEvent.MessageSentEvent(message));
            }
        });
    }

    /**
     * Activate a chat session
     */
    public void activateChat(Long chatId, String chatName) {
        if (chatId == null) {
            log.warn("Cannot activate chat: chatId is null");
            return;
        }

        log.info("Activating chat: {} ({})", chatId, chatName);
        
        // Deactivate current chat if different
        if (currentActiveChatId != null && !currentActiveChatId.equals(chatId)) {
            deactivateChat(currentActiveChatId);
        }
        
        // Create or update chat session
        ChatSession session = activeChats.computeIfAbsent(chatId, k -> new ChatSession(chatId, chatName));
        session.setActive(true);
        session.setLastActivity(System.currentTimeMillis());
        
        currentActiveChatId = chatId;
        
        // Start real-time updates for this chat
        realTimeService.startRealTimeUpdates(chatId);
        
        // Notify UI
        if (chatActivatedCallback != null) {
            chatActivatedCallback.accept(chatId);
        }
        
        // Publish event
        eventBus.publish(new ChatStatusEvent.ChatActiveEvent(chatId));
        
        log.debug("Chat {} activated successfully", chatId);
    }

    /**
     * Deactivate a chat session
     */
    public void deactivateChat(Long chatId) {
        if (chatId == null) {
            return;
        }

        log.info("Deactivating chat: {}", chatId);
        
        ChatSession session = activeChats.get(chatId);
        if (session != null) {
            session.setActive(false);
            session.setLastActivity(System.currentTimeMillis());
        }
        
        if (currentActiveChatId != null && currentActiveChatId.equals(chatId)) {
            currentActiveChatId = null;
        }
        
        // Stop real-time updates for this chat
        realTimeService.stopRealTimeUpdates();
        
        // Notify UI
        if (chatDeactivatedCallback != null) {
            chatDeactivatedCallback.accept(chatId);
        }
        
        // Publish event
        eventBus.publish(new ChatStatusEvent.ChatInactiveEvent(chatId));
        
        log.debug("Chat {} deactivated", chatId);
    }

    /**
     * Send a message
     */
    public void sendMessage(MessageDTO message) {
        if (message == null || message.getChatId() == null) {
            log.warn("Cannot send message: invalid message data");
            return;
        }

        log.debug("Sending message to chat: {}", message.getChatId());
        
        // Update chat session
        ChatSession session = activeChats.get(message.getChatId());
        if (session != null) {
            session.setLastActivity(System.currentTimeMillis());
        }
        
        // Notify observers
        messageObserver.notifyMessageSent(message);
        
        // Update real-time service
        realTimeService.sendMessage(message);
        
        log.debug("Message sent successfully: {}", message.getMessageId());
    }

    /**
     * Handle message received event
     */
    private void handleMessageReceived(MessageEvent.MessageReceivedEvent event) {
        MessageDTO message = event.getMessage();
        Long chatId = message.getChatId();
        
        log.debug("Message received in chat: {}", chatId);
        
        // Update chat session
        ChatSession session = activeChats.get(chatId);
        if (session != null) {
            session.setLastActivity(System.currentTimeMillis());
            session.incrementUnreadCount();
        }
        
        // Notify UI
        if (messageReceivedCallback != null) {
            messageReceivedCallback.accept(message);
        }
        
        // Update notification service
        notificationService.getInstance();
    }

    /**
     * Handle message sent event
     */
    private void handleMessageSent(MessageEvent.MessageSentEvent event) {
        MessageDTO message = event.getMessage();
        
        log.debug("Message sent in chat: {}", message.getChatId());
        
        // Notify UI
        if (messageSentCallback != null) {
            messageSentCallback.accept(message);
        }
    }

    /**
     * Handle message read event
     */
    private void handleMessageRead(MessageEvent.MessageReadEvent event) {
        Long chatId = event.getMessage().getChatId();
        Long userId = event.getReadByUserId();
        
        log.debug("Message read in chat: {} by user: {}", chatId, userId);
        
        // Update chat session
        ChatSession session = activeChats.get(chatId);
        if (session != null) {
            session.clearUnreadCount();
        }
        
        // Update notification service
        notificationService.clearUnreadCount(chatId);
    }

    /**
     * Handle chat active event
     */
    private void handleChatActive(ChatStatusEvent.ChatActiveEvent event) {
        log.debug("Chat {} became active", event.getChatId());
    }

    /**
     * Handle chat inactive event
     */
    private void handleChatInactive(ChatStatusEvent.ChatInactiveEvent event) {
        log.debug("Chat {} became inactive", event.getChatId());
    }

    /**
     * Handle user typing event
     */
    private void handleUserTyping(ChatStatusEvent.UserTypingEvent event) {
        log.debug("User {} is typing in chat {}", event.getUserId(), event.getChatId());
        // Could implement typing indicator in UI
    }

    /**
     * Handle user online event
     */
    private void handleUserOnline(ChatStatusEvent.UserOnlineEvent event) {
        log.debug("User {} online status changed to: {}", event.getUserId(), event.isOnline());
        
        // Notify UI of user status change
        if (userStatusChangedCallback != null) {
            // Create a minimal UserDTO for status update using builder pattern
            UserDTO user = UserDTO.builder()
                .id(event.getUserId())
                .isOnline(event.isOnline())
                .build();
            userStatusChangedCallback.accept(user);
        }
    }

    /**
     * Set message received callback
     */
    public void setMessageReceivedCallback(Consumer<MessageDTO> callback) {
        this.messageReceivedCallback = callback;
    }

    /**
     * Set message sent callback
     */
    public void setMessageSentCallback(Consumer<MessageDTO> callback) {
        this.messageSentCallback = callback;
    }

    /**
     * Set chat activated callback
     */
    public void setChatActivatedCallback(Consumer<Long> callback) {
        this.chatActivatedCallback = callback;
    }

    /**
     * Set chat deactivated callback
     */
    public void setChatDeactivatedCallback(Consumer<Long> callback) {
        this.chatDeactivatedCallback = callback;
    }

    /**
     * Set user status changed callback
     */
    public void setUserStatusChangedCallback(Consumer<UserDTO> callback) {
        this.userStatusChangedCallback = callback;
    }

    /**
     * Get current active chat ID
     */
    public Long getCurrentActiveChatId() {
        return currentActiveChatId;
    }

    /**
     * Get chat session
     */
    public ChatSession getChatSession(Long chatId) {
        return activeChats.get(chatId);
    }

    /**
     * Get all active chats
     */
    public List<ChatSession> getActiveChats() {
        return activeChats.values().stream()
            .filter(ChatSession::isActive)
            .toList();
    }

    /**
     * Check if chat is active
     */
    public boolean isChatActive(Long chatId) {
        ChatSession session = activeChats.get(chatId);
        return session != null && session.isActive();
    }

    /**
     * Get unread count for a chat
     */
    public int getUnreadCount(Long chatId) {
        ChatSession session = activeChats.get(chatId);
        return session != null ? session.getUnreadCount() : 0;
    }

    /**
     * Get total unread count
     */
    public int getTotalUnreadCount() {
        return activeChats.values().stream()
            .mapToInt(ChatSession::getUnreadCount)
            .sum();
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        log.info("Shutting down ChatManagerService");
        
        // Stop real-time updates
        realTimeService.shutdown();
        
        // Shutdown notification service
        notificationService.shutdown();
        
        // Clear active chats
        activeChats.clear();
        currentActiveChatId = null;
        
        isInitialized.set(false);
        
        log.info("ChatManagerService shutdown complete");
    }

    /**
     * Inner class representing a chat session
     */
    public static class ChatSession {
        private final Long chatId;
        private final String chatName;
        private boolean isActive;
        private long lastActivity;
        private int unreadCount;

        public ChatSession(Long chatId, String chatName) {
            this.chatId = chatId;
            this.chatName = chatName;
            this.isActive = false;
            this.lastActivity = System.currentTimeMillis();
            this.unreadCount = 0;
        }

        // Getters and setters
        public Long getChatId() { return chatId; }
        public String getChatName() { return chatName; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
        public int getUnreadCount() { return unreadCount; }
        public void incrementUnreadCount() { unreadCount++; }
        public void clearUnreadCount() { unreadCount = 0; }
    }
}
