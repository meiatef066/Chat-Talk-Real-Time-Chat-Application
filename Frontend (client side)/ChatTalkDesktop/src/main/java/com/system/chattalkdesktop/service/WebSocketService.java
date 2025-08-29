package com.system.chattalkdesktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.ChatDto.SendMessageRequest;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.tyrus.client.ClientManager;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WebSocket service for real-time chat communication
 * Handles STOMP protocol communication with the backend
 */
@Slf4j
@ClientEndpoint
public class WebSocketService {
    private static WebSocketService instance;
    private Session session;
    private final ObjectMapper mapper = JacksonConfig.getObjectMapper();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final EventBus eventBus = EventBus.getInstance();
    private Consumer<MessageDTO> messageHandler;
    private Consumer<String> connectionStatusHandler;
    
    private static final String WS_URL = "ws://localhost:8080/ws";
    private static final String STOMP_CONNECT_FRAME = 
        "CONNECT\n" +
        "accept-version:1.2\n" +
        "heart-beat:10000,10000\n" +
        "authorization:Bearer %s\n" +
        "\n\0";
    
    private static final String STOMP_SUBSCRIBE_FRAME = 
        "SUBSCRIBE\n" +
        "id:sub-0\n" +
        "destination:/user/queue/messages\n" +
        "\n\0";
    
    private static final String STOMP_SEND_FRAME = 
        "SEND\n" +
        "destination:/app/chat.privateMessage\n" +
        "content-type:application/json\n" +
        "content-length:%d\n" +
        "\n%s\0";

    private WebSocketService() {}

    public static synchronized WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }

    /**
     * Connect to WebSocket server
     */
    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            String token = SessionManager.getInstance().getToken();
            if (token == null || token.isEmpty()) {
                log.error("No authentication token available");
                future.complete(false);
                return future;
            }

            ClientManager client = ClientManager.createClient();
            URI uri = URI.create(WS_URL);
            
            client.connectToServer(this, uri);
            
            // Send STOMP CONNECT frame
            String connectFrame = String.format(STOMP_CONNECT_FRAME, token);
            session.getBasicRemote().sendText(connectFrame);
            
            log.info("WebSocket connection established");
            isConnected.set(true);
            future.complete(true);
            
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket: {}", e.getMessage(), e);
            future.complete(false);
        }
        
        return future;
    }

    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("WebSocket connection closed");
            } catch (IOException e) {
                log.error("Error closing WebSocket connection: {}", e.getMessage(), e);
            }
        }
        isConnected.set(false);
    }

    /**
     * Send a message through WebSocket
     */
    public void sendMessage(SendMessageRequest messageRequest) {
        if (!isConnected.get() || session == null || !session.isOpen()) {
            log.warn("WebSocket not connected, cannot send message");
            return;
        }

        try {
            String messageJson = mapper.writeValueAsString(messageRequest);
            String sendFrame = String.format(STOMP_SEND_FRAME, messageJson.length(), messageJson);
            session.getBasicRemote().sendText(sendFrame);
            log.debug("Message sent via WebSocket: {}", messageRequest.getContent());
        } catch (Exception e) {
            log.error("Failed to send message via WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to user-specific message queue
     */
    public void subscribeToMessages() {
        if (!isConnected.get() || session == null || !session.isOpen()) {
            log.warn("WebSocket not connected, cannot subscribe");
            return;
        }

        try {
            session.getBasicRemote().sendText(STOMP_SUBSCRIBE_FRAME);
            log.info("Subscribed to user message queue");
        } catch (IOException e) {
            log.error("Failed to subscribe to messages: {}", e.getMessage(), e);
        }
    }

    /**
     * Set message handler for incoming messages
     */
    public void setMessageHandler(Consumer<MessageDTO> handler) {
        this.messageHandler = handler;
    }

    /**
     * Set connection status handler
     */
    public void setConnectionStatusHandler(Consumer<String> handler) {
        this.connectionStatusHandler = handler;
    }

    /**
     * Check if WebSocket is connected
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    // WebSocket event handlers
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("WebSocket session opened: {}", session.getId());
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("CONNECTED");
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.debug("Received WebSocket message: {}", message);
            
            // Handle STOMP frames
            if (message.startsWith("MESSAGE")) {
                // Extract message body from STOMP frame
                String[] lines = message.split("\n");
                String body = "";
                boolean inBody = false;
                
                for (String line : lines) {
                    if (line.isEmpty() && !inBody) {
                        inBody = true;
                        continue;
                    }
                    if (inBody) {
                        body += line;
                    }
                }
                
                if (!body.isEmpty()) {
                    MessageDTO messageDTO = mapper.readValue(body, MessageDTO.class);
                    handleIncomingMessage(messageDTO);
                }
            } else if (message.startsWith("CONNECTED")) {
                log.info("STOMP connection established");
                subscribeToMessages();
            } else if (message.startsWith("ERROR")) {
                log.error("STOMP error: {}", message);
            }
            
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("WebSocket session closed: {} - {}", session.getId(), closeReason);
        isConnected.set(false);
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("DISCONNECTED");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error in session {}: {}", session.getId(), throwable.getMessage(), throwable);
        isConnected.set(false);
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("ERROR");
        }
    }

    /**
     * Handle incoming message and publish to event bus
     */
    private void handleIncomingMessage(MessageDTO message) {
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
        
        // Publish to event bus for other components
        eventBus.publish(new com.system.chattalkdesktop.event.MessageEvent.MessageReceivedEvent(message));
    }

    /**
     * Send ping to keep connection alive
     */
    public void sendPing() {
        if (isConnected.get() && session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText("PING\n\n\0");
            } catch (IOException e) {
                log.error("Failed to send ping: {}", e.getMessage(), e);
            }
        }
    }
}
