package com.system.chattalkdesktop.NotificationService;

import com.system.chattalkdesktop.enums.NotificationType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationPopupController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Label timestampLabel;

    @FXML
    private ImageView iconImageView;

    @FXML
    private HBox buttonBox;

    @FXML
    private Button acceptButton;

    @FXML
    private Button rejectButton;

    @FXML
    private Button closeButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Rectangle accentLine;

    @FXML
    private Pane root;

    @Getter
    private Notification notification;
    private Runnable onAccept;
    private Runnable onReject;
    private Timeline progressTimeline;

    public void setData(Notification notification, Runnable onAccept, Runnable onReject) {
        this.notification = notification;
        this.onAccept = onAccept;
        this.onReject = onReject;

        updateUI();
        startProgressAnimation();
    }

    private void updateUI() {
        if (notification == null) {
            return;
        }

        // Set title and message
        titleLabel.setText(notification.getTitle());
        messageLabel.setText(notification.getMessage());

        // Set timestamp
        setTimestamp();

        // Set icon based on notification type
        setNotificationIcon(notification.getType());

        // Set background style based on notification type
        setNotificationStyle(notification.getType());

        // Set accent line color
        setAccentLineColor(notification.getType());

        // Show/hide buttons based on notification type
        boolean isFriendRequest = notification.getType() == NotificationType.FRIEND_REQUEST;
        buttonBox.setVisible(isFriendRequest);
        buttonBox.setManaged(isFriendRequest);
    }

    private void setTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        timestampLabel.setText(now.format(formatter));

    }

    private void setNotificationIcon(NotificationType type) {
        String iconPath = "/images/icons/";

        switch (type) {
            case SUCCESS:
                iconPath += "success.png";
                break;
            case ERROR:
                iconPath += "error.png";
                break;
            case FRIEND_REQUEST:
                iconPath += "notification.png";
                break;
            case NEW_MESSAGE:
                iconPath += "message.png";
                break;
            case INFO:
                iconPath += "notification.png";
                break;
            default:
                iconPath += "notification.png";
                break;
        }

        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            iconImageView.setImage(icon);
        } catch (Exception e) {
            // Use default icon if the specific one is not found
            try {
                Image defaultIcon = new Image(getClass().getResourceAsStream("/images/icons/notification.png"));
                iconImageView.setImage(defaultIcon);
            } catch (Exception ex) {
                System.err.println("Could not load notification icon: " + ex.getMessage());
            }
        }
    }

    private void setNotificationStyle(NotificationType type) {
        // Remove existing style classes
        root.getStyleClass().removeIf(styleClass -> styleClass.startsWith("notification-"));

        // Add appropriate style class based on type
        switch (type) {
            case SUCCESS:
                root.getStyleClass().add("notification-success");
                break;
            case ERROR:
                root.getStyleClass().add("notification-error");
                break;
            case FRIEND_REQUEST:
                root.getStyleClass().add("notification-friend-request");
                break;
            case NEW_MESSAGE:
                root.getStyleClass().add("notification-message");
                break;
            case INFO:
                root.getStyleClass().add("notification-info");
                break;
            case MENTION:
                root.getStyleClass().add("notification-mention");
                break;
            case GROUP_INVITE:
                root.getStyleClass().add("notification-group-invite");
                break;
            case MESSAGE_REPLY:
                root.getStyleClass().add("notification-reply");
                break;
            case MESSAGE_REACTION:
                root.getStyleClass().add("notification-reaction");
                break;
            default:
                root.getStyleClass().add("notification-info");
                break;
        }
    }

    private void setAccentLineColor(NotificationType type) {
        String accentColor = "#2196f3"; // Default blue

        switch (type) {
            case SUCCESS:
                accentColor = "#4caf50";
                break;
            case ERROR:
                accentColor = "#f44336";
                break;
            case FRIEND_REQUEST:
                accentColor = "#ff9800";
                break;
            case NEW_MESSAGE:
                accentColor = "#2196f3";
                break;
            case INFO:
                accentColor = "#2196f3";
                break;
        }

        accentLine.setStyle("-fx-fill: " + accentColor + ";");
    }

    private void startProgressAnimation() {
        // Reset progress bar
        progressBar.setProgress(1.0);

        // Create timeline for 4-second countdown
        progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> progressBar.setProgress(1.0)),
                new KeyFrame(Duration.seconds(4), e -> progressBar.setProgress(0.0))
        );

        progressTimeline.setCycleCount(1);
        progressTimeline.play();
    }

    @FXML
    public void handleAccept() {
        if (onAccept != null) {
            onAccept.run();
        }
        closePopup();
    }

    @FXML
    public void handleReject() {
        if (onReject != null) {
            onReject.run();
        }
        closePopup();
    }

    @FXML
    public void handleClose() {
        closePopup();
    }

    private void closePopup() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }

        if (root != null && root.getScene() != null) {
            root.getScene().getWindow().hide();
        }
    }

    public void stopProgressAnimation() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
    }
}
