package com.system.chattalk_serverside.service.RealTimeNotifcation;

import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.enums.NotificationType;
import com.system.chattalk_serverside.model.Notification;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        notification.setTitle("new Friend Request 👀");
        notification.setMessage("Request from "+toUserId.getEmail());
        sendAndSave(toUserId,notification);
    }

    @Override
    public void acceptedFriendRequestNotification( User toUserId, NotificationDTO notification ) {
        notification.setType(NotificationType.FRIEND_RESPONSE_ACCEPTED.name());
        notification.setTitle("your Request Accepted✌");
        notification.setMessage(toUserId.getFirstName()+" accept you friend request");
        sendAndSave(toUserId, notification);
    }

    @Override
    public void rejectedFriendRequestNotification( User toUserId, NotificationDTO notification ) {
        notification.setType(NotificationType.FRIEND_RESPONSE_REJECTED.name());
        notification.setTitle("your Request rejected💔");
        notification.setMessage(toUserId.getFirstName()+" reject you friend request");
        sendAndSave(toUserId, notification);
    }

    @Override
    public void receiveNewMessageNotification( User toUserId,  NotificationDTO notification) {
        notification.setType(NotificationType.NEW_MESSAGE.name());
        notification.setTitle("New message 💌:"+notification.getSenderEmail());
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
