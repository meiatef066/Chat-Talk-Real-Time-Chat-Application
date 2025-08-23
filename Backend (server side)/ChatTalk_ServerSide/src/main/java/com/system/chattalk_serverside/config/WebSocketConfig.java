package com.system.chattalk_serverside.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for STOMP messaging
 * Enables real-time communication between clients and server
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker( MessageBrokerRegistry config) {
        // Enable simple message broker for broadcasting
        config.enableSimpleBroker("/topic", "/queue");

        // Set application destination prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured");
    }

    @Override
    public void registerStompEndpoints( StompEndpointRegistry registry) {
        // Register STOMP endpoints
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure CORS for WebSocket
                .withSockJS(); // Enable SockJS fallback

        // Register WebSocket endpoint without SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        log.info("WebSocket endpoints registered: /ws");
    }

    @Override
    public void configureWebSocketTransport( WebSocketTransportRegistration registration) {
        // Configure transport settings
        registration.setMessageSizeLimit(64 * 1024) // 64KB message size limit
                .setSendBufferSizeLimit(512 * 1024) // 512KB send buffer
                .setSendTimeLimit(20000); // 20 second send time limit

        log.info("WebSocket transport configured");
    }
}
