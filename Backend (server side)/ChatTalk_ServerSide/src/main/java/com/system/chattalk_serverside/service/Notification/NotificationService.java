package com.system.chattalk_serverside.service.Notification;

import com.system.chattalk_serverside.dto.Entity.NotificationDTO;

import java.util.List;

public interface NotificationService {
     List<NotificationDTO> getNotification();
     void deleteAllNotification();
     void deleteNotification(Long notificationId );
     NotificationDTO markAsRead(Long notificationId );
}
