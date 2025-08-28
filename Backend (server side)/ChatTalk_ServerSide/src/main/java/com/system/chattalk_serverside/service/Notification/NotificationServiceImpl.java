package com.system.chattalk_serverside.service.Notification;

import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.model.Notification;
import com.system.chattalk_serverside.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl( NotificationRepository notificationRepository ) {
        super();
        this.notificationRepository = notificationRepository;
    }


    @Override
    public List<NotificationDTO> getNotification() {
        String email = getAuthenticatedEmail();
        List<Notification> notifications = notificationRepository.findByUserEmail(email);

        return notifications.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(this::toNotificationDto).toList();
    }

    @Transactional
    @Override
    public void deleteAllNotification() {
        String email = getAuthenticatedEmail();
        List<Notification> notifications = notificationRepository.findByUserEmail(email);
        if (notifications.isEmpty()) {
            throw new RuntimeException("No notifications found for user: " + email);
        }
        notificationRepository.deleteAll(notifications);
    }

    @Transactional
    @Override
    public void deleteNotification( Long notificationId ) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    @Override
    public NotificationDTO markAsRead( Long notificationId ) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty()) {
            throw new RuntimeException("optionalNotification not exist");
        }
        Notification notification = optionalNotification.get();
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return toNotificationDto(notification);
    }

    private NotificationDTO toNotificationDto( Notification notification ) {
        return NotificationDTO.builder().id(notification.getId())
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null)
                .isRead(notification.getIsRead()!=null?notification.getIsRead():false)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .readAt(notification.getReadAt() != null ? notification.getReadAt().toString() : null)
                .build();
    }

    private String getAuthenticatedEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("No authenticated user found");
        }
        return email;
    }
}
