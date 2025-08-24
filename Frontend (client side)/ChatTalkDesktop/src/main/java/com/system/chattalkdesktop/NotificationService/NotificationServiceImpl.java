package com.system.chattalkdesktop.NotificationService;

import com.system.chattalkdesktop.enums.NotificationType;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NotificationServiceImpl implements NotificationService{
    private static NotificationServiceImpl instance;
    private final List<Stage> activeNotifications = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    private NotificationServiceImpl() {}

    public static NotificationServiceImpl getInstance() {
        if (instance == null) {
            instance = new NotificationServiceImpl();
        }
        return instance;
    }
    @Override
    public void showNotification( NotificationType type, String title, String message) {

        Notification notification =Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .build();
        notification.setId(idCounter.getAndIncrement());
        notifications.add(notification);

        // Play sound for notification
        NotificationSoundPlayer.getInstance().playSoundForNotificationType(type);

        Platform.runLater(() -> {
            try {
                showNotificationPopup(notification, null, null);
            } catch (Exception e) {
                System.err.println("Failed to show notification: " + e.getMessage());
            }
        });
    }

    @Override
    public void showFriendRequestNotification(String senderEmail, Runnable onAccept, Runnable onReject) {
        Notification notification =Notification.builder()
                .title(senderEmail + " sent you a friend request!")
                .message(senderEmail + " sent you a friend request!")
                .type(NotificationType.FRIEND_REQUEST)
                .build();
        notification.setId(idCounter.getAndIncrement());
        notifications.add(notification);

        // Play friend request sound
        NotificationSoundPlayer.getInstance().playFriendRequestSound();

        Platform.runLater(() -> {
            try {
                showNotificationPopup(notification, onAccept, onReject);
            } catch (Exception e) {
                System.err.println("Failed to show friend request notification: " + e.getMessage());
            }
        });
    }

    @Override
    public void showMessageNotification(String senderName, String message) {
        showNotification(NotificationType.NEW_MESSAGE,  senderName, message);
    }

    @Override
    public void showErrorNotification(String title, String message) {
        showNotification(NotificationType.ERROR, title, message);
    }

    @Override
    public void showSuccessNotification(String title, String message) {
        showNotification(NotificationType.SUCCESS, title, message);
    }

    @Override
    public void showInfoNotification(String title, String message) {
        showNotification(NotificationType.INFO, title, message);
    }

    @Override
    public void clearAllNotifications() {
        activeNotifications.forEach(Stage::close);
        activeNotifications.clear();
        notifications.clear();
    }

    @Override
    public List<Notification> getActiveNotifications() {
        return new ArrayList<>(notifications);
    }

    @Override
    public void markAsRead(String notificationId) {
        try {
            Long id = Long.parseLong(notificationId);
            notifications.stream()
                    .filter(n -> id.equals(n.getId()))
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));
        } catch (NumberFormatException e) {
            System.err.println("Invalid notification ID format: " + notificationId);
        }
    }

    @Override
    public void removeNotification(String notificationId) {
        try {
            Long id = Long.parseLong(notificationId);
            notifications.removeIf(n -> id.equals(n.getId()));
        } catch (NumberFormatException e) {
            System.err.println("Invalid notification ID format: " + notificationId);
        }
    }

    @Override
    public void enableSound() {
        NotificationSoundPlayer.getInstance().enableSound();
    }

    @Override
    public void disableSound() {
        NotificationSoundPlayer.getInstance().disableSound();
    }

    @Override
    public boolean isSoundEnabled() {
        return NotificationSoundPlayer.getInstance().isSoundEnabled();
    }

    private void showNotificationPopup(Notification notification, Runnable onAccept, Runnable onReject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/chattalkdesktop/NotificationService/NotificationPopup.fxml"));
            Parent root = loader.load();

            NotificationPopupController controller = loader.getController();
            controller.setData(notification, onAccept, onReject);

            Stage stage = new Stage(StageStyle.UNDECORATED);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);

            // Position notification
            Screen screen = Screen.getPrimary();
            double x = screen.getVisualBounds().getWidth() - 400 - 20;
            double y = screen.getVisualBounds().getHeight() - 140 - 20 - activeNotifications.size() * 150;
            stage.setX(x);
            stage.setY(y);

            activeNotifications.add(stage);

            // Show with animation
            root.setOpacity(0);
            stage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition pause = new PauseTransition(Duration.seconds(4));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                stage.close();
                activeNotifications.remove(stage);
            });

            SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
            sequence.play();

        } catch (Exception e) {
            System.err.println("Failed to show notification popup: " + e.getMessage());
        }
    }
}
