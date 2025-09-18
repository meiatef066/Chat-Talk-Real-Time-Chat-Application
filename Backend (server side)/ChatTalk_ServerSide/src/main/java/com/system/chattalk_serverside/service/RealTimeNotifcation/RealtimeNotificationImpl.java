package com.system.chattalk_serverside.service.RealTimeNotifcation;

import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.enums.NotificationType;
import com.system.chattalk_serverside.model.Notification;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RealtimeNotificationImpl implements RealtimeNotification {
    private SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public RealtimeNotificationImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void receiveFriendRequestNotification( User toUserId, NotificationDTO notification ) {
        notification.setType(NotificationType.FRIEND_REQUEST.name());
        notification.setTitle("New Friend Request 👀");
        notification.setMessage("Friend request from " + notification.getSenderEmail());
        sendAndSave(toUserId, notification);
    }

    @Override
    public void acceptedFriendRequestNotification( User toUserId, NotificationDTO notification ) {
        notification.setType(NotificationType.FRIEND_RESPONSE_ACCEPTED.name());
        notification.setTitle("Friend Request Accepted ✌");
        notification.setMessage(toUserId.getFirstName() + " accepted your friend request");
        sendAndSave(toUserId, notification);
    }

    @Override
    public void rejectedFriendRequestNotification( User toUserId, NotificationDTO notification ) {
        notification.setType(NotificationType.FRIEND_RESPONSE_REJECTED.name());
        notification.setTitle("Friend Request Rejected 💔");
        notification.setMessage(toUserId.getFirstName() + " rejected your friend request");
        sendAndSave(toUserId, notification);
    }

    @Override
    public void receiveNewMessageNotification( User toUserId,  NotificationDTO notification) {
        notification.setType(NotificationType.NEW_MESSAGE.name());
        notification.setTitle("New Message 💌");
        notification.setMessage("New message from " + notification.getSenderEmail());
        sendAndSave(toUserId, notification);
    }


    private void sendAndSave(User toUserId, NotificationDTO notification) {
        saveNotification(toUserId, notification);
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSendToUser(
                    toUserId.getEmail(),
                    "/queue/notifications",
                    notification
            );
        }
        System.out.println("Notification sent"+notification.getTitle()+" "+toUserId.getEmail());
    }

    @Override
    public void sendMessageToUser(User toUserId, MessageDTO message) {
        if (messagingTemplate != null && toUserId != null && message != null) {
            try {
                // Send message via both channels for compatibility
                messagingTemplate.convertAndSendToUser(
                    toUserId.getEmail(),
                    "/queue/messages",
                    message
                );
                
                messagingTemplate.convertAndSendToUser(
                    toUserId.getEmail(),
                    "/queue/chat",
                    message
                );
                
                System.out.println("Message sent to user: " + toUserId.getEmail());
            } catch (Exception e) {
                System.err.println("Failed to send message to user " + toUserId.getEmail() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void sendMessageToAllParticipants(List<User> participants, MessageDTO message) {
        if (participants != null && !participants.isEmpty() && message != null) {
            participants.forEach(participant -> sendMessageToUser(participant, message));
        }
    }

    @Override
    public void sendMessageToUserById(Long userId, MessageDTO message) {
        if (userId != null && message != null) {
            // This method would need a UserRepository to fetch the user by ID
            // For now, we'll just log that this method needs implementation
            System.out.println("sendMessageToUserById called for userId: " + userId + " - needs UserRepository implementation");
        }
    }

    private void saveNotification(User toUser, NotificationDTO notification) {
        Notification entity = Notification.builder()
                .title(notification.getTitle())
                .createdAt(LocalDateTime.now())
                .message(notification.getMessage())
                .type(NotificationType.valueOf(notification.getType()))
                .user(toUser)
                .build();
        notificationRepository.save(entity);
    }

}
