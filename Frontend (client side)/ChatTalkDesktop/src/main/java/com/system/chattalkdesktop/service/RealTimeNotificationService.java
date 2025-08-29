package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.entity.NotificationDTO;
import com.system.chattalkdesktop.event.MessageEvent;
import com.system.chattalkdesktop.event.ChatStatusEvent;
import com.system.chattalkdesktop.utils.SessionManager;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time notification service for desktop notifications
 * Integrates with the observer pattern and provides system notifications
 */
@Slf4j
public class RealTimeNotificationService {
    private static RealTimeNotificationService instance;
    private final EventBus eventBus = EventBus.getInstance();
    private final ChatMessageObserver messageObserver = ChatMessageObserver.getInstance();
    private final ConcurrentHashMap<Long, AtomicInteger> unreadCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lastNotificationTime = new ConcurrentHashMap<>();
    
    // Notification cooldown to prevent spam (5 seconds)
    private static final long NOTIFICATION_COOLDOWN = 5000;
    
    // System tray support
    private TrayIcon trayIcon;
    private boolean systemTraySupported = false;

    private RealTimeNotificationService() {
        initializeSystemTray();
        setupEventSubscriptions();
        setupMessageObserver();
    }

    public static synchronized RealTimeNotificationService getInstance() {
        if (instance == null) {
            instance = new RealTimeNotificationService();
        }
        return instance;
    }

    /**
     * Initialize system tray for notifications
     */
    private void initializeSystemTray() {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/images/icons/notification.png")
                );
                
                if (image == null) {
                    // Fallback to default icon
                    image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                }
                
                trayIcon = new TrayIcon(image, "ChatTalk Desktop");
                trayIcon.setImageAutoSize(true);
                
                // Add action listener for notification clicks
                trayIcon.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // TODO: Bring chat window to front
                        log.info("Notification clicked, bringing chat to front");
                    }
                });
                
                tray.add(trayIcon);
                systemTraySupported = true;
                log.info("System tray initialized successfully");
                
            } catch (AWTException e) {
                log.error("Failed to initialize system tray: {}", e.getMessage(), e);
                systemTraySupported = false;
            }
        } else {
            log.warn("System tray not supported on this platform");
            systemTraySupported = false;
        }
    }

    /**
     * Setup event subscriptions
     */
    private void setupEventSubscriptions() {
        // Subscribe to message events
        eventBus.subscribe(MessageEvent.MessageReceivedEvent.class, this::handleNewMessage);
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
                handleNewMessage(new MessageEvent.MessageReceivedEvent(message));
            }

            @Override
            public void onMessageRead(Long chatId, Long userId) {
                handleMessageRead(new MessageEvent.MessageReadEvent(
                    new MessageDTO(), userId
                ));
            }

            @Override
            public void onMessageSent(MessageDTO message) {
                // Don't show notification for sent messages
            }
        });
    }

    /**
     * Handle new message event
     */
    private void handleNewMessage(MessageEvent.MessageReceivedEvent event) {
        MessageDTO message = event.getMessage();
        Long chatId = message.getChatId();
        Long senderId = message.getSenderId();
        
        // Check if message is from current user
        Long currentUserId = SessionManager.getInstance().getCurrentUser() != null ? 
            SessionManager.getInstance().getCurrentUser().getId() : null;
        
        if (currentUserId != null && senderId.equals(currentUserId)) {
            return; // Don't notify for own messages
        }
        
        // Update unread count
        unreadCounts.compute(chatId, (k, v) -> {
            if (v == null) v = new AtomicInteger(0);
            v.incrementAndGet();
            return v;
        });
        
        // Check notification cooldown
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastNotificationTime.get(chatId);
        
        if (lastTime == null || (currentTime - lastTime) > NOTIFICATION_COOLDOWN) {
            showMessageNotification(message);
            lastNotificationTime.put(chatId, currentTime);
        }
        
        // Update tray icon tooltip
        updateTrayIconTooltip();
    }

    /**
     * Handle message read event
     */
    private void handleMessageRead(MessageEvent.MessageReadEvent event) {
        Long chatId = event.getMessage().getChatId();
        
        // Reset unread count for this chat
        unreadCounts.remove(chatId);
        
        // Update tray icon tooltip
        updateTrayIconTooltip();
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
        if (event.isTyping()) {
            log.debug("User {} is typing in chat {}", event.getUserId(), event.getChatId());
            // Could show typing indicator in UI
        }
    }

    /**
     * Handle user online event
     */
    private void handleUserOnline(ChatStatusEvent.UserOnlineEvent event) {
        if (event.isOnline()) {
            log.debug("User {} came online", event.getUserId());
            // Could show online status in UI
        }
    }

    /**
     * Show desktop notification for new message
     */
    private void showMessageNotification(MessageDTO message) {
        if (!systemTraySupported || trayIcon == null) {
            log.warn("System tray not available for notifications");
            return;
        }

        try {
            String title = "New Message";
            String content = message.getContent();
            
            // Truncate long messages
            if (content.length() > 100) {
                content = content.substring(0, 97) + "...";
            }
            
            // Show notification
            trayIcon.displayMessage(
                title,
                content,
                TrayIcon.MessageType.INFO
            );
            
            log.debug("Desktop notification shown for message: {}", message.getMessageId());
            
        } catch (Exception e) {
            log.error("Failed to show desktop notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Update tray icon tooltip with unread count
     */
    private void updateTrayIconTooltip() {
        if (!systemTraySupported || trayIcon == null) {
            return;
        }

        int totalUnread = unreadCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        String tooltip = "ChatTalk Desktop";
        if (totalUnread > 0) {
            tooltip += " (" + totalUnread + " unread)";
        }
        
        trayIcon.setToolTip(tooltip);
    }

    /**
     * Get unread count for a specific chat
     */
    public int getUnreadCount(Long chatId) {
        AtomicInteger count = unreadCounts.get(chatId);
        return count != null ? count.get() : 0;
    }

    /**
     * Get total unread count across all chats
     */
    public int getTotalUnreadCount() {
        return unreadCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
    }

    /**
     * Clear unread count for a specific chat
     */
    public void clearUnreadCount(Long chatId) {
        unreadCounts.remove(chatId);
        updateTrayIconTooltip();
    }

    /**
     * Clear all unread counts
     */
    public void clearAllUnreadCounts() {
        unreadCounts.clear();
        updateTrayIconTooltip();
    }

    /**
     * Show custom notification
     */
    public void showCustomNotification(String title, String message, TrayIcon.MessageType type) {
        if (!systemTraySupported || trayIcon == null) {
            log.warn("System tray not available for notifications");
            return;
        }

        try {
            trayIcon.displayMessage(title, message, type);
        } catch (Exception e) {
            log.error("Failed to show custom notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if system tray is supported
     */
    public boolean isSystemTraySupported() {
        return systemTraySupported;
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        log.info("RealTimeNotificationService shutdown complete");
    }
}
