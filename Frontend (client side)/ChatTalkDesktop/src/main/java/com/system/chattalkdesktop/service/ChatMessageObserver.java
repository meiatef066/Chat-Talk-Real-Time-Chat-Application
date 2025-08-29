package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.event.*;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Observer pattern implementation for chat message updates
 * Manages subscriptions and notifications for message-related events
 */
@Slf4j
public class ChatMessageObserver {

    private static volatile ChatMessageObserver instance;
    private final List<MessageUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final EventBus eventBus;
    private final AtomicBoolean isHandlingEventBusEvent = new AtomicBoolean(false);

    private ChatMessageObserver() {
        this.eventBus = EventBus.getInstance();
        setupEventSubscriptions();
    }

    public static ChatMessageObserver getInstance() {
        if (instance == null) {
            synchronized (ChatMessageObserver.class) {
                if (instance == null) {
                    instance = new ChatMessageObserver();
                }
            }
        }
        return instance;
    }

    /**
     * Add a listener for message updates
     */
    public void addListener(MessageUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Added message update listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Remove a listener for message updates
     */
    public void removeListener(MessageUpdateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            log.debug("Removed message update listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all listeners of a new message
     */
    public void notifyNewMessage(MessageDTO message) {
        if (message == null) {
            log.warn("Attempted to notify with null message");
            return;
        }
        
        log.debug("Notifying {} listeners of new message: {}", listeners.size(), message.getMessageId());
        listeners.forEach(listener -> {
            try {
                listener.onNewMessage(message);
            } catch (Exception e) {
                log.error("Error notifying listener {}: {}", listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        });

        // Publish to event bus for other components (but don't create circular dependency)
        // Only publish if this is not already handling an event from the event bus
        if (isHandlingEventBusEvent.compareAndSet(false, true)) {
            try {
                eventBus.publish(new MessageEvent.MessageReceivedEvent(message));
            } finally {
                isHandlingEventBusEvent.set(false);
            }
        }
    }

    /**
     * Notify all listeners that messages were read
     */
    public void notifyMessageRead(Long chatId, Long userId) {
        if (chatId == null || userId == null) {
            log.warn("Attempted to notify message read with null chatId or userId");
            return;
        }
        
        log.debug("Notifying {} listeners of message read in chat: {}", listeners.size(), chatId);
        listeners.forEach(listener -> {
            try {
                listener.onMessageRead(chatId, userId);
            } catch (Exception e) {
                log.error("Error notifying listener {}: {}", listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        });
    }

    /**
     * Notify all listeners of a message being sent
     */
    public void notifyMessageSent(MessageDTO message) {
        if (message == null) {
            log.warn("Attempted to notify with null message");
            return;
        }
        
        log.debug("Notifying {} listeners of message sent: {}", listeners.size(), message.getMessageId());
        listeners.forEach(listener -> {
            try {
                listener.onMessageSent(message);
            } catch (Exception e) {
                log.error("Error notifying listener {}: {}", listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        });

        // Publish to event bus for other components (but don't create circular dependency)
        // Only publish if this is not already handling an event from the event bus
        if (isHandlingEventBusEvent.compareAndSet(false, true)) {
            try {
                eventBus.publish(new MessageEvent.MessageSentEvent(message));
            } finally {
                isHandlingEventBusEvent.set(false);
            }
        }
    }

    /**
     * Setup event subscriptions for incoming messages
     */
    private void setupEventSubscriptions() {
        // Subscribe to message events from the event bus
        eventBus.subscribe(MessageEvent.MessageReceivedEvent.class, this::handleMessageReceivedEvent);
        eventBus.subscribe(MessageEvent.MessageSentEvent.class, this::handleMessageSentEvent);
        eventBus.subscribe(MessageEvent.MessageReadEvent.class, this::handleMessageReadEvent);
    }

    /**
     * Handle message received events from event bus
     */
    private void handleMessageReceivedEvent(MessageEvent.MessageReceivedEvent event) {
        if (event != null && event.getMessage() != null) {
            notifyNewMessage(event.getMessage());
        }
    }

    /**
     * Handle message sent events from event bus
     */
    private void handleMessageSentEvent(MessageEvent.MessageSentEvent event) {
        if (event != null && event.getMessage() != null) {
            notifyMessageSent(event.getMessage());
        }
    }

    /**
     * Handle message read events from event bus
     */
    private void handleMessageReadEvent(MessageEvent.MessageReadEvent event) {
        if (event != null && event.getMessage() != null) {
            notifyMessageRead(event.getMessage().getChatId(), event.getReadByUserId());
        }
    }

    /**
     * Get the number of active listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clear all listeners
     */
    public void clearListeners() {
        listeners.clear();
        log.info("Cleared all message update listeners");
    }
}