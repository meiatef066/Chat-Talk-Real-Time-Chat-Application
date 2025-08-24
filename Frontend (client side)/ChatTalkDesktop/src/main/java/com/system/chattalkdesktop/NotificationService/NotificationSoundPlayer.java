package com.system.chattalkdesktop.NotificationService;

import com.system.chattalkdesktop.enums.NotificationType;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSoundPlayer {

    private static NotificationSoundPlayer instance;
    private MediaPlayer friendRequestPlayer;
    private MediaPlayer notificationPlayer;
    private MediaPlayer errorPlayer;
    @Getter
    private boolean soundEnabled = true;

    private NotificationSoundPlayer() {
        initializeSoundPlayers();
    }

    public static NotificationSoundPlayer getInstance() {
        if (instance == null) {
            instance = new NotificationSoundPlayer();
        }
        return instance;
    }

    private void initializeSoundPlayers() {
        try {
            // Initialize friend request sound
            String friendRequestPath = getClass().getResource("/sounds/friend_request.wav").toExternalForm();
            Media friendRequestMedia = new Media(friendRequestPath);
            friendRequestPlayer = new MediaPlayer(friendRequestMedia);

            // Initialize general notification sound
            String notificationPath = getClass().getResource("/sounds/notifications.wav").toExternalForm();
            Media notificationMedia = new Media(notificationPath);
            notificationPlayer = new MediaPlayer(notificationMedia);

            // Initialize error sound
            String errorPath = getClass().getResource("/sounds/error.wav").toExternalForm();
            Media errorMedia = new Media(errorPath);
            errorPlayer = new MediaPlayer(errorMedia);

        } catch (Exception e) {
            System.err.println("Failed to initialize sound players: " + e.getMessage());
        }
    }

    public void playFriendRequestSound() {
        playSound(friendRequestPlayer);
    }

    public void playNotificationSound() {
        playSound(notificationPlayer);
    }

    public void playErrorSound() {
        playSound(errorPlayer);
    }

    private void playSound(MediaPlayer player) {
        if (player != null && soundEnabled) {
            try {
                player.stop();
                player.play();
            } catch (Exception e) {
                System.err.println("Failed to play sound: " + e.getMessage());
            }
        }
    }

    public void playSoundForNotificationType( NotificationType type) {
        switch (type) {
            case FRIEND_REQUEST:
                playFriendRequestSound();
                break;
            case ERROR:
                playErrorSound();
                break;
            case NEW_MESSAGE:
            case SUCCESS:
            case INFO:
            default:
                playNotificationSound();
                break;
        }
    }

    public void dispose() {
        if (friendRequestPlayer != null) {
            friendRequestPlayer.dispose();
        }
        if (notificationPlayer != null) {
            notificationPlayer.dispose();
        }
        if (errorPlayer != null) {
            errorPlayer.dispose();
        }
    }

    public void enableSound() {
        soundEnabled = true;
    }

    public void disableSound() {
        soundEnabled = false;
    }

}