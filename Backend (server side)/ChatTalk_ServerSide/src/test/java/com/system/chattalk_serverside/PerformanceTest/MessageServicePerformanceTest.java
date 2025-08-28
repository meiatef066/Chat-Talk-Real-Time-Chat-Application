package com.system.chattalk_serverside.PerformanceTest;

import com.system.chattalk_serverside.dto.ChatDto.SendMessageRequest;
import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import com.system.chattalk_serverside.model.Chat;
import com.system.chattalk_serverside.model.Message;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.ChatRepository;
import com.system.chattalk_serverside.repository.MessageRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.Message.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServicePerformanceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User authenticatedUser;
    private Chat chat;

    @BeforeEach
    void setup() {
        authenticatedUser = new User();
        authenticatedUser.setId(10L);
        authenticatedUser.setEmail("user@example.com");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(authenticatedUser, null));
        SecurityContextHolder.setContext(context);

        chat = Chat.builder().id(100L).name("Test Chat").createdBy(authenticatedUser).build();
        chat.setParticipants(new java.util.HashSet<>());
    }

    @Test
    void sendMessage_Performance_ConcurrentRequests() throws Exception {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(authenticatedUser));

        Message saved = Message.builder()
                .id(1L)
                .chat(chat)
                .sender(authenticatedUser)
                .content("test message")
                .messageType(com.system.chattalk_serverside.enums.MessageType.TEXT)
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        int numberOfRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfRequests];
        for (int i = 0; i < numberOfRequests; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    SendMessageRequest dto = SendMessageRequest.builder()
                            .chatId(100L)
                            .content("Message " + index)
                            .build();
                    MessageDTO result = messageService.sendMessage(dto);
                    assertNotNull(result);
                } catch (Exception e) {
                    fail("Test failed with exception: " + e.getMessage());
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        verify(messageRepository, times(numberOfRequests)).save(any(Message.class));
        
        assertTrue(totalTime < 5000, "Performance test took too long: " + totalTime + "ms");
        double avgTimePerRequest = (double) totalTime / numberOfRequests;
        assertTrue(avgTimePerRequest < 50, "Average time per request too high: " + avgTimePerRequest + "ms");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
