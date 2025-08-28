package com.system.chattalk_serverside.UnitTest;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceImplTest {

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
    }

    @Test
    void sendMessage_shouldThrow_whenUserNotInChat() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(false);

        SendMessageRequest dto = SendMessageRequest.builder().chatId(100L).content("hi").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> messageService.sendMessage(dto));
        assertTrue(ex.getMessage().contains("not a participant"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_shouldPersist_whenValid() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(authenticatedUser));

        // Initialize participants set to avoid NPE
        chat.setParticipants(new java.util.HashSet<>());

        Message saved = Message.builder()
                .id(1L)
                .chat(chat)
                .sender(authenticatedUser)
                .content("hi")
                .messageType(com.system.chattalk_serverside.enums.MessageType.TEXT)
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        SendMessageRequest dto = SendMessageRequest.builder().chatId(100L).content("hi").build();
        MessageDTO result = messageService.sendMessage(dto);

        assertNotNull(result);
        assertEquals(1L, result.getMessageId());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertEquals("hi", captor.getValue().getContent());
    }

    @Test
    void editMessage_shouldValidateChatAndSender() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(true);

        Message msg = Message.builder()
                .id(5L)
                .chat(chat)
                .sender(authenticatedUser)
                .content("old")
                .messageType(com.system.chattalk_serverside.enums.MessageType.TEXT)
                .build();
        when(messageRepository.findById(5L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        MessageDTO updated = messageService.editMessage(100L, 5L, "new", 10L);
        assertEquals("new", updated.getContent());
    }

    @Test
    void editMessage_shouldThrow_whenMessageNotInChat() {
        Chat otherChat = Chat.builder().id(200L).name("Other").build();
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(true);
        Message msg = Message.builder()
                .id(5L)
                .chat(otherChat)
                .sender(authenticatedUser)
                .content("old")
                .messageType(com.system.chattalk_serverside.enums.MessageType.TEXT)
                .build();
        when(messageRepository.findById(5L)).thenReturn(Optional.of(msg));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> messageService.editMessage(100L, 5L, "new", 10L));
        assertTrue(ex.getMessage().contains("does not belong"));
    }

    @Test
    void deleteMessage_forEveryone_shouldRequireSenderOrOwner() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(true);
        
        // Create a different owner for the chat
        User chatOwner = new User(); 
        chatOwner.setId(999L);
        chat.setCreatedBy(chatOwner);
        
        User otherSender = new User(); 
        otherSender.setId(99L);
        Message msg = Message.builder()
                .id(5L)
                .chat(chat)
                .sender(otherSender)
                .content("x")
                .messageType(com.system.chattalk_serverside.enums.MessageType.TEXT)
                .build();
        when(messageRepository.findById(5L)).thenReturn(Optional.of(msg));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> messageService.deleteMessage(100L, 5L, 10L, true));
        verify(messageRepository, never()).deleteById(anyLong());
    }

    @Test
    void markConversationAsRead_shouldValidateMembership() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(100L, 10L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> messageService.markConversationAsRead(100L, 10L));
        verify(messageRepository, never()).markConversationAsReadForUser(anyLong(), anyLong());
    }
}


