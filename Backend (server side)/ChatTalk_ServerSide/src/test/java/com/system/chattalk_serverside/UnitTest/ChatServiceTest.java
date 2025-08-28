package com.system.chattalk_serverside.UnitTest;

import com.system.chattalk_serverside.dto.Entity.ChatDto;
import com.system.chattalk_serverside.enums.ChatType;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.Chat;
import com.system.chattalk_serverside.model.ChatParticipation;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.Message;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.ChatRepository;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.Chat.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User user1;
    private User user2;
    private Chat chat;
    private ChatParticipation participation1;
    private ChatParticipation participation2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .email("user1@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        user2 = User.builder()
                .id(2L)
                .email("user2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        chat = Chat.builder()
                .id(1L)
                .name("Private: John & Jane")
                .chatType(ChatType.PRIVATE)
                .createdBy(user1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        participation1 = ChatParticipation.builder()
                .id(1L)
                .chat(chat)
                .user(user1)
                .role(ChatParticipation.ParticipationRole.MEMBER)
                .status(ChatParticipation.ParticipationStatus.ACTIVE)
                .build();

        participation2 = ChatParticipation.builder()
                .id(2L)
                .chat(chat)
                .user(user2)
                .role(ChatParticipation.ParticipationRole.MEMBER)
                .status(ChatParticipation.ParticipationStatus.ACTIVE)
                .build();

        chat.setParticipants(new HashSet<>(Arrays.asList(participation1, participation2)));

        // Set up security context for authenticated user
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(user1, null));
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getPrivateChat_ExistingChat_ReturnsChatId() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(any(), any(), any())).thenReturn(true);
        when(chatRepository.findPrivateChatBetweenUsers(1L, 2L)).thenReturn(Optional.of(chat));

        // When
        Long result = chatService.GetPrivateChat("user1@example.com", "user2@example.com");

        // Then
        assertThat(result).isEqualTo(1L);
        verify(chatRepository).findPrivateChatBetweenUsers(1L, 2L);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void getPrivateChat_NewChat_CreatesAndReturnsChatId() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(any(), any(), any())).thenReturn(true);
        when(chatRepository.findPrivateChatBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // When
        Long result = chatService.GetPrivateChat("user1@example.com", "user2@example.com");

        // Then
        assertThat(result).isEqualTo(1L);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void getPrivateChat_UsersNotFriends_ThrowsException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(any(), any(), any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.GetPrivateChat("user1@example.com", "user2@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Users cannot chat - they are not friends");
    }

    @Test
    void getPrivateChat_UserNotFound_ThrowsException() {
        // Stub the first user (existing)
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        // Stub the second user (nonexistent)
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.GetPrivateChat("user1@example.com", "nonexistent@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getPrivateChat_EmptyEmails_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> chatService.GetPrivateChat("", "user2@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email addresses cannot be null or empty");

        assertThatThrownBy(() -> chatService.GetPrivateChat("user1@example.com", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email addresses cannot be null or empty");
    }

    @Test
    void getPrivateChat_SameUser_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> chatService.GetPrivateChat("user1@example.com", "user1@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot create chat with yourself");
    }

    @Test
    void privateChatExists_ExistingChat_ReturnsTrue() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(chatRepository.findPrivateChatBetweenUsers(1L, 2L)).thenReturn(Optional.of(chat));

        // When
        boolean result = chatService.privateChatExists("user1@example.com", "user2@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void privateChatExists_NoChat_ReturnsFalse() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(chatRepository.findPrivateChatBetweenUsers(1L, 2L)).thenReturn(Optional.empty());

        // When
        boolean result = chatService.privateChatExists("user1@example.com", "user2@example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void privateChatExists_UserNotFound_ReturnsFalse() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        boolean result = chatService.privateChatExists("user1@example.com", "nonexistent@example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void updateChatName_ValidRequest_UpdatesChatName() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(1L, 1L)).thenReturn(true);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // When
        ChatDto result = chatService.updateChatName(1L, "New Chat Name", "user1@example.com");

        // Then
        assertThat(result.getChatName()).isEqualTo("New Chat Name");
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void updateChatName_UserNotInChat_ThrowsException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.updateChatName(1L, "New Name", "user1@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User is not a participant in this chat");
    }

    @Test
    void updateChatName_EmptyName_ThrowsException() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.isUserInChat(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.updateChatName(1L, "", "user1@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat name cannot be empty");
    }

    @Test
    void deletePrivateChat_ValidRequest_DeletesChat() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        // When
        chatService.deletePrivateChat(1L, "user1@example.com");

        // Then
        verify(chatRepository).delete(chat);
    }

    @Test
    void deletePrivateChat_UserNotInChat_ThrowsException() {
        Chat anotherChat = Chat.builder()
                .id(2L)
                .name("Another Chat")
                .chatType(ChatType.PRIVATE)
                .createdBy(user2)
                .participants(new HashSet<>(Collections.singletonList(participation2))) // only user2
                .build();

        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.findById(2L)).thenReturn(Optional.of(anotherChat));

        assertThatThrownBy(() -> chatService.deletePrivateChat(2L, "user1@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User is not a participant in this chat");
    }

    @Test
    void getChatMessageCount_ValidChat_ReturnsCount() {
        // Given
        chat.setMessages(Arrays.asList(
                Message.builder().id(1L).build(),
                Message.builder().id(2L).build(),
                Message.builder().id(3L).build()
        ));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        // When
        Long result = chatService.getChatMessageCount(1L);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    void getUserChatCount_ValidUser_ReturnsCount() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.countChatsByUserId(1L)).thenReturn(5L);

        // When
        Long result = chatService.getUserChatCount("user1@example.com");

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void getChatParticipants_ValidChat_ReturnsParticipants() {
        // Given
        when(chatRepository.getChatParticipantEmails(1L)).thenReturn(Arrays.asList("user1@example.com", "user2@example.com"));

        // When
        List<String> result = chatService.getChatParticipants(1L);

        // Then
        assertThat(result).containsExactly("user1@example.com", "user2@example.com");
    }

    @Test
    void isUserInChat_UserInChat_ReturnsTrue() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.isUserInChat(1L, 1L)).thenReturn(true);

        // When
        boolean result = chatService.isUserInChat(1L, "user1@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUserInChat_UserNotInChat_ReturnsFalse() {
        // Given
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(chatRepository.isUserInChat(1L, 1L)).thenReturn(false);

        // When
        boolean result = chatService.isUserInChat(1L, "user1@example.com");

        // Then
        assertThat(result).isFalse();
    }
}
