package com.system.chattalkdesktop.notificationPage;

import com.system.chattalkdesktop.Dto.entity.NotificationDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationItem {
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Label timestampLabel;
    @FXML private Label statusLabel;
    @FXML private Label senderLabel;
    @FXML private Label typeLabel;
    @FXML private Button markAsReadButton;
    @FXML private Button deleteButton;
    @FXML private ImageView icon;

    private NotificationDTO notification;
    private NotificationController parentController;

    public void setData(NotificationDTO notification, NotificationController parentController) {
        this.notification = notification;
        this.parentController = parentController;

        // Set title (use message if title is null or empty)
        String title = (notification.getTitle() != null && !notification.getTitle().trim().isEmpty()) 
            ? notification.getTitle() 
            : "Notification";
        titleLabel.setText(title);

        // Set message
        messageLabel.setText(notification.getMessage());

        // Set sender information based on type
        String sender = getSenderFromType(notification.getType());
        senderLabel.setText("from " + notification.getUserId());

        // Set type with proper formatting
        String typeDisplay = formatNotificationType(notification.getType());
        typeLabel.setText(typeDisplay);

        // Format timestamp
        String formattedTime = formatTimestamp(notification.getCreatedAt());
        timestampLabel.setText(formattedTime);

        updateStatus(notification.isRead());
    }

    private String getSenderFromType(String type) {
        if (type == null) return "System";
        
        switch (type.toLowerCase()) {
            case "message":
                return "Chat Message";
            case "friend_request":
                return "Friend Request";
            case "group_invite":
                return "Group Invitation";
            case "system":
                return "System";
            case "alert":
                return "Alert";
            default:
                return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        }
    }

    private String formatNotificationType(String type) {
        if (type == null) return "System";
        
        switch (type.toLowerCase()) {
            case "message":
                return "💬 Message";
            case "friend_request":
                return "👥 Friend Request";
            case "group_invite":
                return "👥 Group Invite";
            case "system":
                return "⚙️ System";
            case "alert":
                return "⚠️ Alert";
            default:
                return "📢 " + type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        }
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "Just now";
        }
        
        try {
            // Try to parse the timestamp and format it nicely
            LocalDateTime dateTime = LocalDateTime.parse(timestamp.replace(" ", "T"));
            LocalDateTime now = LocalDateTime.now();
            
            if (dateTime.toLocalDate().equals(now.toLocalDate())) {
                // Same day - show time only
                return "Today at " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else if (dateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                // Yesterday
                return "Yesterday at " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                // Other days - show date and time
                return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
            }
        } catch (Exception e) {
            // If parsing fails, return the original timestamp
            return timestamp;
        }
    }

    private void updateStatus(boolean isRead) {
        if (isRead) {
            statusLabel.setText("✅ Read");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            markAsReadButton.setVisible(false);
        } else {
            statusLabel.setText("🔵 Unread");
            statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
            markAsReadButton.setVisible(true);
        }
    }

    @FXML
    private void onMarkAsReadClicked() {
        markAsRead();
    }

    @FXML
    private void deleteNotification() {
        parentController.deleteNotification(notification.getId());
    }

    // Getter for messageLabel (used in filtering)
    public Label getMessageLabel() {
        return messageLabel;
    }

    // Getter for titleLabel (used in filtering)
    public Label getTitleLabel() {
        return titleLabel;
    }

    // Getter for notification (used by parent controller)
    public NotificationDTO getNotification() {
        return notification;
    }

    // Public method to mark as read (used by parent controller)
    public void markAsRead() {
        updateStatus(true);
        parentController.markNotificationAsRead(notification.getId());
    }
}