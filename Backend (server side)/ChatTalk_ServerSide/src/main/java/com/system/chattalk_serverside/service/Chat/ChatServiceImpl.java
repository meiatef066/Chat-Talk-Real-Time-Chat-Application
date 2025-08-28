package com.system.chattalk_serverside.service.Chat;

import com.system.chattalk_serverside.dto.Entity.ChatDto;
import com.system.chattalk_serverside.enums.ChatType;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.Chat;
import com.system.chattalk_serverside.model.ChatParticipation;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.ChatRepository;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService{
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public ChatServiceImpl( FriendRequestRepository friendRequestRepository, UserRepository userRepository, ChatRepository chatRepository ) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    @Transactional
    @Override
    public Long GetPrivateChat( String email1, String email2 ) {
        // Validate input parameters
        validateChatRequest(email1, email2);
        
        User user1 = getUserByEmail(email1);
        User user2 = getUserByEmail(email2);

        // Check if users are friends
        validateFriendship(user1, user2);

        // Try to find existing chat
        Optional<Chat> existingChat = chatRepository.findPrivateChatBetweenUsers(user1.getId(), user2.getId());
        if (existingChat.isPresent()) {
            Chat chat = existingChat.get();
            log.info("Found existing private chat: {} between users: {} and {}",
                    chat.getId(), email1, email2);
            return chat.getId();
        }

        // Create new chat if none exists
        return createNewPrivateChat(user1, user2);
    }

    @Override
    public List<Long> getAllUserPrivateChatId() {
        String currentUserEmail = getCurrentUserEmail();
        User currentUser = getUserByEmail(currentUserEmail);
        
        return chatRepository.findPrivateChatsByUserId(currentUser.getId())
                .stream()
                .map(Chat::getId)
                .toList();
    }

    @Override
    public Page<ChatDto> searchChatsByName() {
        return searchChatsByName("", PageRequest.of(0, 20));
    }

    @Override
    public List<ChatDto> getUserPrivateChats() {
        String currentUserEmail = getCurrentUserEmail();
        User currentUser = getUserByEmail(currentUserEmail);
        
        return chatRepository.findPrivateChatsByUserId(currentUser.getId())
                .stream()
                .map(this::convertToChatDto)
                .toList();
    }

    @Override
    public List<ChatDto> getUserAllChats() {
        String currentUserEmail = getCurrentUserEmail();
        User currentUser = getUserByEmail(currentUserEmail);
        
        return chatRepository.findChatsByUserId(currentUser.getId())
                .stream()
                .map(this::convertToChatDto)
                .toList();
    }

    @Override
    public Optional<ChatDto> getChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .map(this::convertToChatDto);
    }

    @Transactional
    @Override
    public ChatDto updateChatName(Long chatId, String newName, String userEmail) {
        User user = getUserByEmail(userEmail);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        // Verify user is participant in this chat
        if (!isUserInChat(chatId, userEmail)) {
            throw new RuntimeException("User is not a participant in this chat");
        }
        
        // Validate new name
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat name cannot be empty");
        }
        
        chat.setName(newName.trim());
        chat.setUpdatedAt(LocalDateTime.now());
        Chat savedChat = chatRepository.save(chat);
        
        log.info("Updated chat name: {} to '{}' by user: {}", chatId, newName, userEmail);
        return convertToChatDto(savedChat);
    }

    @Override
    public boolean privateChatExists(String email1, String email2) {
        try {
            User user1 = getUserByEmail(email1);
            User user2 = getUserByEmail(email2);
            
            return chatRepository.findPrivateChatBetweenUsers(user1.getId(), user2.getId()).isPresent();
        } catch (Exception e) {
            log.warn("Error checking if private chat exists between {} and {}: {}", email1, email2, e.getMessage());
            return false;
        }
    }

    @Override
    public Page<ChatDto> searchChatsByName(String searchTerm, Pageable pageable) {
        String currentUserEmail = getCurrentUserEmail();
        User currentUser = getUserByEmail(currentUserEmail);
        
        return chatRepository.searchChatsByName(currentUser.getId(), searchTerm, pageable)
                .map(this::convertToChatDto);
    }

    @Override
    public List<ChatDto> getChatsByType(String chatType) {
        String currentUserEmail = getCurrentUserEmail();
        User currentUser = getUserByEmail(currentUserEmail);

        return chatRepository.findChatsByTypeAndUserId(currentUser.getId(), chatType)
                .stream()
                .map(this::convertToChatDto)
                .toList();
    }

    @Override
    public List<ChatDto> getRecentChats(String userEmail, int limit) {
        User user = getUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(0, limit);
        
        return chatRepository.findRecentChatsByUserId(user.getId(), pageable)
                .stream()
                .map(this::convertToChatDto)
                .toList();
    }

    @Override
    public boolean isUserInChat(Long chatId, String userEmail) {
        User user = getUserByEmail(userEmail);
        return chatRepository.isUserInChat(chatId, user.getId());
    }

    @Override
    public List<String> getChatParticipants(Long chatId) {
        return chatRepository.getChatParticipantEmails(chatId);
    }

    @Transactional
    @Override
    public void leaveChat(Long chatId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        // Find user's participation
        ChatParticipation participation = chat.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a participant in this chat"));
        
        // Update participation status
        participation.setStatus(ChatParticipation.ParticipationStatus.LEFT);
        participation.setLeftAt(LocalDateTime.now());
        
        log.info("User {} left chat: {}", userEmail, chatId);
    }

    @Override
    public Long getChatMessageCount(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return (long) chat.getMessages().size();
    }

    @Override
    public Long getUserChatCount(String userEmail) {
        User user = getUserByEmail(userEmail);
        return chatRepository.countChatsByUserId(user.getId());
    }

    /**
     * Delete a private chat (for cleanup purposes)
     */
    @Transactional
    public void deletePrivateChat(Long chatId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        // Verify user is participant in this chat
        boolean isParticipant = chat.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));
        
        if (!isParticipant) {
            throw new RuntimeException("User is not a participant in this chat");
        }
        
        // Verify it's a private chat
        if (chat.getChatType() != ChatType.PRIVATE) {
            throw new RuntimeException("Can only delete private chats");
        }
        
        chatRepository.delete(chat);
        log.info("Deleted private chat: {} by user: {}", chatId, userEmail);
    }

    // Private helper methods

    private void validateChatRequest(String email1, String email2) {
        if (email1 == null || email2 == null || email1.trim().isEmpty() || email2.trim().isEmpty()) {
            throw new IllegalArgumentException("Email addresses cannot be null or empty");
        }
        
        if (email1.equals(email2)) {
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    private void validateFriendship(User user1, User user2) {
        if (!areUsersFriends(user1, user2)) {
            throw new RuntimeException("Users cannot chat - they are not friends");
        }
    }

    private Long createNewPrivateChat(User user1, User user2) {
        log.info("Creating new private chat between users: {} and {}", 
                user1.getEmail(), user2.getEmail());
        
        Chat chat = Chat.builder()
                .name("Private: " + user1.getFirstName() + " & " + user2.getFirstName())
                .chatType(ChatType.PRIVATE)
                .createdBy(user1)
                .createdAt(LocalDateTime.now())
                .build();
        
        Set<ChatParticipation> participation = Set.of(
                ChatParticipation.builder().chat(chat).user(user1).role(ChatParticipation.ParticipationRole.MEMBER).build(),
                ChatParticipation.builder().chat(chat).user(user2).role(ChatParticipation.ParticipationRole.MEMBER).build()
        );
        chat.setParticipants(participation);
        
        Chat savedChat = chatRepository.save(chat);
        
        log.info("Created new private chat: {} between users: {} and {}",
                savedChat.getId(), user1.getEmail(), user2.getEmail());
        return savedChat.getId();
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user");
        }
        return auth.getName();
    }

    /**
     * Check if two users are friends
     */
    private boolean areUsersFriends( User user1, User user2) {
        return friendRequestRepository.existsBySenderAndReceiverAndStatus(user1, user2, FriendRequest.RequestStatus.ACCEPTED) ||
                friendRequestRepository.existsBySenderAndReceiverAndStatus(user2, user1, FriendRequest.RequestStatus.ACCEPTED);
    }

    /**
     * Convert Chat entity to DTO
     */
    private ChatDto convertToChatDto( Chat chat) {
        return ChatDto.builder()
                .chatId(chat.getId())
                .chatName(chat.getName())
                .chatType(chat.getChatType().name())
                .participationEmail(chat.getParticipants().stream()
                        .map(participation -> participation.getUser().getEmail())
                        .toList())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }
}
