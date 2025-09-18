package com.system.chattalk_serverside.service.User;

import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.model.*;
import com.system.chattalk_serverside.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling user account deletion with proper cascade handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {
    
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final ChatParticipationRepository chatParticipationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final ChatRepository chatRepository;

    /**
     * Soft delete user account - marks user as deleted but keeps data for referential integrity
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        log.info("Starting soft deletion for user: {} ({})", user.getEmail(), userId);

        // 1. Mark user as deleted
        user.setStatus(UserStatus.DELETED);
        user.setIsOnline(false);
        user.setEmail("deleted_" + userId + "_" + user.getEmail());
        user.setUsername("deleted_" + userId + "_" + user.getUsername());
        userRepository.save(user);

        // 2. Handle friend requests - mark as cancelled
        List<FriendRequest> sentRequests = friendRequestRepository.findBySenderId(userId);
        List<FriendRequest> receivedRequests = friendRequestRepository.findByReceiverId(userId);
        
        for (FriendRequest request : sentRequests) {
            if (request.getStatus() == FriendRequest.RequestStatus.PENDING) {
                request.setStatus(FriendRequest.RequestStatus.CANCELLED);
                friendRequestRepository.save(request);
            }
        }
        
        for (FriendRequest request : receivedRequests) {
            if (request.getStatus() == FriendRequest.RequestStatus.PENDING) {
                request.setStatus(FriendRequest.RequestStatus.CANCELLED);
                friendRequestRepository.save(request);
            }
        }

        // 3. Handle chat participations - mark as left
        List<ChatParticipation> participations = chatParticipationRepository.findByUserId(userId);
        for (ChatParticipation participation : participations) {
            participation.setStatus(ChatParticipation.ParticipationStatus.LEFT);
            participation.setLeftAt(LocalDateTime.now());
            chatParticipationRepository.save(participation);
        }

        // 4. Handle messages - anonymize sender
        List<Message> userMessages = messageRepository.findBySenderId(userId);
        for (Message message : userMessages) {
            message.setContent("[Message from deleted user]");
            messageRepository.save(message);
        }

        // 5. Delete user's notifications
        List<Notification> userNotifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(userNotifications);

        log.info("Soft deletion completed for user: {} ({})", user.getEmail(), userId);
    }

    /**
     * Hard delete user account - completely removes user and all related data
     * WARNING: This will permanently delete all user data
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        log.info("Starting hard deletion for user: {} ({})", user.getEmail(), userId);

        // 1. Delete user's notifications
        List<Notification> userNotifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(userNotifications);

        // 2. Delete user's messages
        List<Message> userMessages = messageRepository.findBySenderId(userId);
        messageRepository.deleteAll(userMessages);

        // 3. Delete chat participations
        List<ChatParticipation> participations = chatParticipationRepository.findByUserId(userId);
        chatParticipationRepository.deleteAll(participations);

        // 4. Delete friend requests where user is sender or receiver
        List<FriendRequest> sentRequests = friendRequestRepository.findBySenderId(userId);
        List<FriendRequest> receivedRequests = friendRequestRepository.findByReceiverId(userId);
        friendRequestRepository.deleteAll(sentRequests);
        friendRequestRepository.deleteAll(receivedRequests);

        // 5. Delete private chats where user is the only participant
        List<Chat> userChats = chatRepository.findByCreatedById(userId);
        for (Chat chat : userChats) {
            if (chat.getChatType() == com.system.chattalk_serverside.enums.ChatType.PRIVATE) {
                // Check if chat has other participants
                long participantCount = chatParticipationRepository.countByChatId(chat.getId());
                if (participantCount <= 1) {
                    chatRepository.delete(chat);
                }
            }
        }

        // 6. Finally delete the user
        userRepository.delete(user);

        log.info("Hard deletion completed for user: {} ({})", user.getEmail(), userId);
    }

    /**
     * Check if user can be safely deleted
     */
    public boolean canDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Check if user has any active participations in group chats
        List<ChatParticipation> activeParticipations = chatParticipationRepository
                .findByUserIdAndStatus(userId, ChatParticipation.ParticipationStatus.ACTIVE);
        
        // Check if user is the only admin in any group chat
        for (ChatParticipation participation : activeParticipations) {
            if (participation.getRole() == ChatParticipation.ParticipationRole.ADMIN) {
                long adminCount = chatParticipationRepository.countByChatIdAndRole(
                        participation.getChat().getId(), 
                        ChatParticipation.ParticipationRole.ADMIN
                );
                if (adminCount <= 1) {
                    return false; // Cannot delete - user is the only admin
                }
            }
        }
        
        return true;
    }
}
