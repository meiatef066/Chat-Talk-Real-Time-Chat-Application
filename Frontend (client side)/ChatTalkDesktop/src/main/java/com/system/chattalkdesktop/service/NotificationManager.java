package com.system.chattalkdesktop.service;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.entity.NotificationDTO;
import com.system.chattalkdesktop.MainChat.FriendListController;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.service.ChatMessageObserver;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.application.Platform;
import lombok.Setter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NotificationManager {
    private static NotificationManager instance;
    private StompSession stompSession;
    private WebSocketStompClient stompClient;
    private boolean isConnecting = false;
    private boolean isConnected = false;

    // Reference to FriendListController for updating chat data
    @Setter
    private FriendListController friendListController;

    // Reference to NotificationController for real-time updates
    @Setter
    private com.system.chattalkdesktop.notificationPage.NotificationController notificationController;

    // Notification control flags
    @Setter
    private boolean showConnectionNotifications = true;
    @Setter
    private boolean showDebugNotifications = false;

    private final String websocketUrl = System.getProperty("websocket.url", "ws://localhost:8080/ws");

    private NotificationManager() {
        // Don't connect immediately - use lazy initialization
        if (showDebugNotifications) {
            System.out.println("🔧 NotificationManager instance created (lazy initialization)");
        }
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            if (instance == null) {
                instance = new NotificationManager();
            }
        }
        return instance;
    }

    private void connectWebSocket() {
        if (isConnecting || isConnected) {
            if (showDebugNotifications) {
                System.out.println("🔒 WebSocket connection already in progress or connected");
            }
            return;
        }

        if (showDebugNotifications) {
            System.out.println("🔌 Attempting to connect to WebSocket at: " + websocketUrl);
        }
        isConnecting = true;

        WebSocketClient client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);

        // Configure message converters to handle various payload types
        MappingJackson2MessageConverter jsonConverter = new MappingJackson2MessageConverter();
        // Don't set a specific payload class to allow flexibility
        stompClient.setMessageConverter(jsonConverter);

        // Enable automatic message conversion
        stompClient.setAutoStartup(true);

        if (showDebugNotifications) {
            System.out.println("🔧 Message converter configured: " + jsonConverter.getClass().getSimpleName());
        }

        try {
            String token = SessionManager.getInstance().getToken();
            if (token == null || token.isEmpty()) {
                System.err.println("⚠️ No valid JWT token available");
                if (showConnectionNotifications) {
                    Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                            "WebSocket Error",
                            "No valid authentication token available"
                    ));
                }
                isConnecting = false;
                return;
            }

            if (showDebugNotifications) {
                System.out.println("🔑 JWT token available, length: " + token.length());
            }
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            if (showDebugNotifications) {
                System.out.println("📋 Authorization header added: Bearer " + token.substring(0, Math.min(20, token.length())) + "...");
            }

            int maxRetries = 3;
            int retryDelayMs = 5000;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    if (showDebugNotifications) {
                        System.out.println("🔄 WebSocket connection attempt " + attempt + "/" + maxRetries);
                    }
                    stompSession = stompClient.connectAsync(
                            websocketUrl,
                            headers,
                            new SessionHandler()
                    ).get(10, TimeUnit.SECONDS);
                    if (showDebugNotifications) {
                        System.out.println("✅ WebSocket connected successfully on attempt " + attempt);
                    }
                    isConnecting = false;
                    isConnected = true;
                    return;
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("⚠️ WebSocket connection failed (attempt " + attempt + "): " + e.getMessage());
                    if (showDebugNotifications) {
                        e.printStackTrace();
                    }
                    if (attempt == maxRetries) {
                        if (showConnectionNotifications) {
                            Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                                    "WebSocket Connection Failed",
                                    "Could not connect to WebSocket server after " + maxRetries + " attempts"
                            ));
                        }
                        isConnecting = false;
                        throw new RuntimeException("❌ Failed to connect to WebSocket server", e);
                    }
                    try {
                        if (showDebugNotifications) {
                            System.out.println("⏳ Waiting " + retryDelayMs + "ms before retry...");
                        }
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } catch (TimeoutException e) {
                    System.err.println("⏰ WebSocket connection timed out on attempt " + attempt);
                    isConnecting = false;
                    throw new RuntimeException("❌ WebSocket connection timed out", e);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Critical error in connectWebSocket: " + e.getMessage());
            if (showDebugNotifications) {
                e.printStackTrace();
            }
            isConnecting = false;
            throw new RuntimeException("❌ Failed to connect to WebSocket server", e);
        }
    }

    public void subscribeToNotifications() {
        if (showDebugNotifications) {
            System.out.println("📡 subscribeToNotifications() called");
        }
        if (!isConnected) {
            if (showDebugNotifications) {
                System.out.println("🔌 Not connected, attempting to connect...");
            }
            try {
                connectWebSocket();
            } catch (Exception e) {
                System.err.println("⚠️ Failed to connect to WebSocket: " + e.getMessage());
                if (showConnectionNotifications) {
                    Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                            "WebSocket Connection Failed",
                            "Could not connect to notification service"
                    ));
                }
                return;
            }
        }

        if (isConnected) {
            try {
                if (showDebugNotifications) {
                    System.out.println("✅ Connected, subscribing to notifications...");
                }

                // Subscribe to general notifications
                subscribeToGeneralNotifications();

                // Subscribe to chat messages
                subscribeToChatMessages();

                if (showDebugNotifications) {
                    System.out.println("📬 Subscribed to notifications and chat messages");
                }

            } catch (Exception e) {
                System.err.println("⚠️ Failed to subscribe to notifications: " + e.getMessage());
                if (showDebugNotifications) {
                    e.printStackTrace();
                }
                if (showConnectionNotifications) {
                    Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                            "Subscription Failed",
                            "Could not subscribe to notifications"
                    ));
                }
            }
        } else {
            System.err.println("⚠️ Cannot subscribe, WebSocket not connected");
            if (showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "Subscription Failed",
                        "WebSocket is not connected"
                ));
            }
        }
    }

    private void subscribeToGeneralNotifications() {
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType( StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                NotificationDTO notification = (NotificationDTO) payload;
                if (showDebugNotifications) {
                    System.out.println("📩 Received notification: " + notification);
                }
                Platform.runLater(() -> {
                    try {
                        // Show desktop notification
                        switch (notification.getType()) {
                            case "FRIEND_REQUEST" -> NotificationServiceImpl.getInstance().showFriendRequestNotification(
                                    notification.getTitle(),
                                    () -> {
                                        if (showDebugNotifications) {
                                            System.out.println("✅ Friend request accepted from " + notification.getTitle());
                                        }
                                        // TODO: Call REST API to accept friend request
                                    },
                                    () -> {
                                        if (showDebugNotifications) {
                                            System.out.println("❌ Friend request rejected from " + notification.getTitle());
                                        }
                                        // TODO: Call REST API to reject friend request
                                    }
                            );
                            case "FRIEND_RESPONSE_ACCEPTED" ->
                                    NotificationServiceImpl.getInstance().showSuccessNotification(
                                            notification.getTitle(),
                                            notification.getMessage()
                                    );
                            case "FRIEND_RESPONSE_REJECTED" ->
                                    NotificationServiceImpl.getInstance().showErrorNotification(
                                            notification.getTitle(),
                                            notification.getMessage()
                                    );
                            case "NEW_MESSAGE" -> NotificationServiceImpl.getInstance().showMessageNotification(
                                    notification.getTitle(),
                                    notification.getMessage()
                            );
                            default -> NotificationServiceImpl.getInstance().showInfoNotification(
                                    notification.getTitle(),
                                    notification.getMessage()
                            );
                        }
                        
                        // Update notification list in real-time if controller is available
                        if (notificationController != null) {
                            notificationController.addNewNotification(notification);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error handling notification: " + e.getMessage());
                        if (showDebugNotifications) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void subscribeToChatMessages() {
        // Subscribe to private chat messages
        stompSession.subscribe("/user/queue/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                MessageDTO message = (MessageDTO) payload;
                if (showDebugNotifications) {
                    System.out.println("💬 Received chat message via WebSocket: " + message);
                    System.out.println("💬 Message details - ID: " + message.getMessageId() + 
                                     ", Content: " + message.getContent() + 
                                     ", Sender: " + message.getSenderId() + 
                                     ", Chat: " + message.getChatId());
                }

                Platform.runLater(() -> {
                    try {
                        // Only show notification for new messages, not for own messages
                        Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                        if (message.getSenderId() != null && currentUserId != null &&
                                !message.getSenderId().equals(currentUserId)) {
                            NotificationServiceImpl.getInstance().showMessageNotification(
                                    "New Message",
                                    message.getContent()
                            );
                        }

                        // Update friend list with new message if controller is available
                        if (friendListController != null && message.getSenderId() != null) {
                            updateChatWithNewMessage(message);
                        }
                        
                        // Also notify ChatMessageObserver for real-time updates
                        ChatMessageObserver.getInstance().notifyNewMessage(message);
                        
                    } catch (Exception e) {
                        System.err.println("⚠️ Error handling chat message: " + e.getMessage());
                        if (showDebugNotifications) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // Also subscribe to general message notifications
        stompSession.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                MessageDTO message = (MessageDTO) payload;
                if (showDebugNotifications) {
                    System.out.println("💬 Received message via /user/queue/messages: " + message);
                }

                Platform.runLater(() -> {
                    try {
                        // Notify ChatMessageObserver for real-time updates
                        ChatMessageObserver.getInstance().notifyNewMessage(message);
                        
                        // Update friend list if controller is available
                        if (friendListController != null) {
                            updateChatWithNewMessage(message);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error handling message from /user/queue/messages: " + e.getMessage());
                        if (showDebugNotifications) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // Subscribe to public chat messages (if needed)
        stompSession.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                if (showDebugNotifications) {
                    System.out.println("📢 Received public message: " + message);
                }

                // Only show public message notifications if explicitly enabled
                if (showDebugNotifications) {
                    Platform.runLater(() -> {
                        NotificationServiceImpl.getInstance().showInfoNotification(
                                "Public Message",
                                message
                        );
                    });
                }
            }
        });
    }

    private void updateChatWithNewMessage(MessageDTO message) {
        if (message == null || friendListController == null) {
            if (showDebugNotifications) {
                System.out.println("💬 Cannot update chat: message or controller is null");
            }
            return;
        }

        if (showDebugNotifications) {
            System.out.println("💬 Updating chat with new message: " + message.getContent());
        }

        try {
            // Get the sender's email to identify which friend sent the message
            Long senderId = message.getSenderId();
            Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
            
            if (senderId == null || currentUserId == null || senderId.equals(currentUserId)) {
                // Don't update for own messages
                return;
            }

            // Update the friend list with new message info
            Platform.runLater(() -> {
                try {
                    friendListController.handleNewMessage(message);
                    
                    if (showDebugNotifications) {
                        System.out.println("✅ Chat updated with new message from user: " + senderId);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error updating friend list with new message: " + e.getMessage());
                    if (showDebugNotifications) {
                        e.printStackTrace();
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in updateChatWithNewMessage: " + e.getMessage());
            if (showDebugNotifications) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test basic WebSocket connectivity without sending/receiving messages
     * This is a simpler test that just verifies the connection is working
     */
    public void testConnection() {
        if (!isConnected()) {
            connectWebSocket();
        }

        if (isConnected()) {
            try {
                // Just test basic connectivity
                if (showDebugNotifications) {
                    System.out.println("✅ WebSocket connection test successful");
                }
                if (showConnectionNotifications) {
//                Platform.runLater(() -> NotificationServiceImpl.getInstance().showSuccessNotification(
//                        "Connection Test",
//                        "WebSocket connection is working"
//                ));
                }

            } catch (Exception e) {
                System.err.println("⚠️ Error during connection test: " + e.getMessage());
                if (showConnectionNotifications) {
                    Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                            "Connection Test Error",
                            "Failed to test connection"
                    ));
                }
            }
        } else {
            System.err.println("⚠️ Cannot test connection, WebSocket not connected");
            if (showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "Connection Test Failed",
                        "WebSocket is not connected"
                ));
            }
        }
    }

    /**
     * Test ping/pong functionality with the WebSocket server
     * This method can be used to test message sending/receiving capabilities
     * Note: This may cause message conversion errors if the server response format doesn't match expectations
     */
    public void testPingPong() {
        if (!isConnected()) {
            if (showDebugNotifications) {
                System.out.println("⚠️ Cannot test ping/pong, WebSocket not connected");
            }
            return;
        }

        try {
            // Subscribe to pong messages
            stompSession.subscribe("/user/queue/pong", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (showDebugNotifications) {
                        System.out.println("📬 Pong received: " + payload);
                    }

                    if (showConnectionNotifications) {
                        Platform.runLater(() -> NotificationServiceImpl.getInstance().showSuccessNotification(
                                "Ping/Pong Test",
                                "Response: " + payload
                        ));
                    }
                }
            });

            // Send ping
            stompSession.send("/app/ping", "Ping");
            if (showDebugNotifications) {
                System.out.println("📍 Ping sent");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Ping/pong test failed: " + e.getMessage());
            if (showDebugNotifications) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String destination, Object message) {
        if (!isConnected()) {
            connectWebSocket();
        }

        if (isConnected()) {
            stompSession.send(destination, message);
        } else {
            System.err.println("⚠️ Cannot send message, session not connected");
            if (showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "Send Failed",
                        "WebSocket is not connected"
                ));
            }
        }
    }

    public void sendNotification(Object payload) {
        if (!isConnected()) {
            connectWebSocket();
        }

        if (isConnected()) {
            stompSession.send("/app/notify", payload);
        } else {
            System.err.println("⚠️ Not connected, cannot send notification");
            if (showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "Notification Failed",
                        "WebSocket is not connected"
                ));
            }
        }
    }

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected() && isConnected;
    }

    private static class SessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            if (instance != null && instance.showDebugNotifications) {
                System.out.println("✅ WebSocket connected successfully");
                System.out.println("📋 Connected headers: " + connectedHeaders);
            }
            if (instance != null && instance.showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showSuccessNotification(
                        "WebSocket Connected",
                        "Successfully connected to real-time notifications"
                ));
            }
        }

        @Override
        public void handleException( StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable ex) {
            System.err.println("❌ WebSocket error: " + ex.getMessage());
            if (instance != null && instance.showDebugNotifications) {
                System.err.println("🔍 Command: " + command + ", Headers: " + headers);
                if (payload != null) {
                    System.err.println("📦 Payload length: " + payload.length);
                }
                ex.printStackTrace();
            }
            if (instance != null && instance.showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "WebSocket Error",
                        "Failed to process WebSocket message"
                ));
            }
        }

        @Override
        public void handleTransportError(StompSession session, Throwable ex) {
            System.err.println("⚠️ Transport error: " + ex.getMessage());
            if (instance != null && instance.showDebugNotifications) {
                System.err.println("🔍 Session: " + session);
                ex.printStackTrace();
            }
            if (instance != null && instance.showConnectionNotifications) {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification(
                        "WebSocket Connection Failed",
                        "Unable to connect to WebSocket server"
                ));
            }
        }
    }
}