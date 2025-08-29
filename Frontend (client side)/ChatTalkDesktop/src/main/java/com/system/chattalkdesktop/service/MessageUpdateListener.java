package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;

/**
 * Interface for components that want to listen to message updates
 * Part of the Observer pattern implementation
 */
public interface MessageUpdateListener {

    /**
     * Called when a new message is received
     */
    void onNewMessage( MessageDTO message);

    /**
     * Called when messages are marked as read
     */
    void onMessageRead(Long chatId, Long userId);

    /**
     * Called when a message is sent
     */
    void onMessageSent(MessageDTO message);
}