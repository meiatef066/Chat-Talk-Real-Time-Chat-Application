package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.MainChat.APIService.ApiChatService;
import com.system.chattalkdesktop.utils.SessionManager;
import com.system.chattalkdesktop.event.ChatStatusEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/**
 * Real-time chat service that provides live updates using polling
 * Integrates with the existing API services and observer pattern
 */
@Slf4j
public class RealTimeChatService {
    private static RealTimeChatService instance;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ChatMessageObserver messageObserver = ChatMessageObserver.getInstance();
    private final EventBus eventBus = EventBus.getInstance();
    
    // Polling intervals
    private static final int MESSAGE_POLL_INTERVAL = 2000; // 2 seconds
    private static final int STATUS_POLL_INTERVAL = 5000; // 5 seconds
    
    // Current chat context
    private Long currentChatId;
    private Long lastMessageId;
    private ScheduledFuture<?> messagePollingTask;
    private ScheduledFuture<?> statusPollingTask;

    private RealTimeChatService() {}

    public static synchronized RealTimeChatService getInstance() {
        if (instance == null) {
            instance = new RealTimeChatService();
        }
        return instance;
    }

    /**
     * Start real-time updates for a specific chat
     */
    public void startRealTimeUpdates(Long chatId) {
        if (isRunning.get() && currentChatId != null && currentChatId.equals(chatId)) {
            log.debug("Real-time updates already running for chat: {}", chatId);
            return;
        }

        stopRealTimeUpdates();
        
        this.currentChatId = chatId;
        this.lastMessageId = 0L;
        
        log.info("Starting real-time updates for chat: {}", chatId);
        
        // Start message polling
        startMessagePolling();
        
        // Start status polling
        startStatusPolling();
        
        isRunning.set(true);
        
        // Publish event
        eventBus.publish(new ChatStatusEvent.ChatActiveEvent(chatId));
    }

    /**
     * Stop real-time updates
     */
    public void stopRealTimeUpdates() {
        if (!isRunning.get()) {
            return;
        }

        log.info("Stopping real-time updates");
        
        if (messagePollingTask != null && !messagePollingTask.isCancelled()) {
            messagePollingTask.cancel(false);
        }
        
        if (statusPollingTask != null && !statusPollingTask.isCancelled()) {
            statusPollingTask.cancel(false);
        }
        
        isRunning.set(false);
        currentChatId = null;
        lastMessageId = null;
        
        // Publish event
        if (currentChatId != null) {
            eventBus.publish(new ChatStatusEvent.ChatInactiveEvent(currentChatId));
        }
    }

    /**
     * Start polling for new messages
     */
    private void startMessagePolling() {
        messagePollingTask = scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get() || currentChatId == null) {
                return;
            }

            try {
                pollForNewMessages();
            } catch (Exception e) {
                log.error("Error polling for new messages: {}", e.getMessage(), e);
            }
        }, 0, MESSAGE_POLL_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Start polling for chat status updates
     */
    private void startStatusPolling() {
        statusPollingTask = scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get() || currentChatId == null) {
                return;
            }

            try {
                pollForStatusUpdates();
            } catch (Exception e) {
                log.error("Error polling for status updates: {}", e.getMessage(), e);
            }
        }, 0, STATUS_POLL_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Poll for new messages
     */
    private void pollForNewMessages() {
        if (currentChatId == null) {
            return;
        }

        try {
            // Get recent messages (last 10)
            Task<List<MessageDTO>> task = ApiChatService.getChatHistory(currentChatId, 0, 10);
            
            task.setOnSucceeded(event -> {
                try {
                    List<MessageDTO> messages = task.getValue();
                    if (messages != null && !messages.isEmpty()) {
                        processNewMessages(messages);
                    }
                } catch (Exception e) {
                    log.error("Error processing polled messages: {}", e.getMessage(), e);
                }
            });
            
            task.setOnFailed(event -> {
                log.error("Failed to poll for messages: {}", task.getException().getMessage());
                // Don't show notification for polling failures to avoid spam
            });

            // Execute on background thread
            new Thread(task).start();
            
        } catch (Exception e) {
            log.error("Error setting up message polling task: {}", e.getMessage(), e);
        }
    }

    /**
     * Process new messages and notify observers
     */
    private void processNewMessages(List<MessageDTO> messages) {
        for (MessageDTO message : messages) {
            // Check if this is a new message
            if (lastMessageId == null || message.getMessageId() > lastMessageId) {
                // Update last message ID
                if (lastMessageId == null || message.getMessageId() > lastMessageId) {
                    lastMessageId = message.getMessageId();
                }
                
                // Check if message is from current user
                Long currentUserId = SessionManager.getInstance().getCurrentUser() != null ? 
                    SessionManager.getInstance().getCurrentUser().getId() : null;
                if (currentUserId != null && !message.getSenderId().equals(currentUserId)) {
                    // This is a new message from another user
                    log.debug("New message received: {}", message.getMessageId());
                    messageObserver.notifyNewMessage(message);
                }
            }
        }
    }

    /**
     * Poll for status updates (online status, typing indicators, etc.)
     */
    private void pollForStatusUpdates() {
        if (currentChatId == null) {
            return;
        }

        // TODO: Implement status polling when backend provides status endpoints
        // For now, just log that we're checking status
        log.debug("Polling for status updates in chat: {}", currentChatId);
    }

    /**
     * Send a message and handle real-time updates
     */
    public void sendMessage(MessageDTO message) {
        if (currentChatId == null || !currentChatId.equals(message.getChatId())) {
            log.warn("Cannot send message: chat not active or chat ID mismatch");
            return;
        }

        // Notify observers that message was sent
        messageObserver.notifyMessageSent(message);
        
        // Update last message ID
        if (lastMessageId == null || message.getMessageId() > lastMessageId) {
            lastMessageId = message.getMessageId();
        }
    }

    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(Long chatId, Long userId) {
        if (currentChatId != null && currentChatId.equals(chatId)) {
            messageObserver.notifyMessageRead(chatId, userId);
        }
    }

    /**
     * Get current chat ID
     */
    public Long getCurrentChatId() {
        return currentChatId;
    }

    /**
     * Check if real-time updates are running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        stopRealTimeUpdates();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RealTimeChatService shutdown complete");
    }

    /**
     * Set message handler for incoming messages
     */
    public void setMessageHandler(Consumer<MessageDTO> handler) {
        // This will be handled through the observer pattern
        // The handler can subscribe to the ChatMessageObserver
    }

    /**
     * Get connection status
     */
    public String getConnectionStatus() {
        if (isRunning.get()) {
            return "CONNECTED";
        } else {
            return "DISCONNECTED";
        }
    }
}
