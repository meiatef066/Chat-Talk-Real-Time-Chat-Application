package com.system.chattalkdesktop.NotificationService;

import java.util.List;

public interface NotificationService {
    void showNotification(NotificationType type, String title, String message);
    void showFriendRequestNotification(String senderEmail, Runnable onAccept, Runnable onReject);
    void showMessageNotification(String senderName, String message);
    void showErrorNotification(String title, String message);
    void showSuccessNotification(String title, String message);

    void showInfoNotification(String title, String message);

    void clearAllNotifications();

    List<Notification> getActiveNotifications();

    void markAsRead(String notificationId);

    void removeNotification(String notificationId);

    // Sound control methods
    void enableSound();
    void disableSound();
    boolean isSoundEnabled();

}
