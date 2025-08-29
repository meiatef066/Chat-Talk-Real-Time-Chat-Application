package com.system.chattalkdesktop.MainChat;

import com.system.chattalkdesktop.Dto.entity.UserDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class ChatUserItemController {
    @FXML
    public ImageView avatar;
    @FXML
    public Label statusLabel;
    @FXML
    public HBox chatItem;
    @FXML
    private Label userName;
    @FXML
    private Label userEmail;
    @FXML
    private Label lastMessage;
    @FXML
    private Label unreadCountLabel;
    @FXML
    private Label timestampLabel;
    @FXML
    private Circle statusIndicator;
    @FXML
    private Label typingIndicator;

    private UserDTO user;
    private Long chatId;
    private int unreadCount = 0;
    private String lastMessageText = "";
    private String lastMessageTime = "";
    private boolean isTyping = false;
    private Timeline typingAnimation;

    public void setUserData(UserDTO user) {
        try {
            this.user = user;
            userName.setText(user.getFirstName() + " " + user.getLastName());
            userEmail.setText(user.getEmail());
            
            // Load profile image
            loadProfileImage(user.getProfilePictureUrl());
            
            // Update online status
            updateOnlineStatus(user.getIsOnline());
            
            // Initialize default values
            lastMessage.setText("No messages yet");
            unreadCountLabel.setText("");
            timestampLabel.setText("");
            typingIndicator.setVisible(false);

            // Setup hover effects
            setupHoverEffects();
            
            // Initialize typing animation
            initializeTypingAnimation();

        } catch (Exception e) {
            System.err.println("Error setting user data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load profile image with fallback
     */
    private void loadProfileImage(String profilePictureUrl) {
        try {
            if (profilePictureUrl != null && !profilePictureUrl.trim().isEmpty()) {
                Image profileImage = new Image(profilePictureUrl);
                avatar.setImage(profileImage);
            } else {
                // Set default avatar
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/icons2/icons8-male-user-50.png"));
                avatar.setImage(defaultImage);
            }
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            // Set default avatar on error
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/icons2/icons8-male-user-50.png"));
                avatar.setImage(defaultImage);
            } catch (Exception ex) {
                System.err.println("Error loading default avatar: " + ex.getMessage());
            }
        }
    }

    /**
     * Update online status with visual indicator
     */
    public void updateOnlineStatus(Boolean isOnline) {
        if (isOnline != null && isOnline) {
            // Online status
            statusLabel.setText("Online");
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 12px;");
            statusIndicator.setStyle("-fx-fill: #4CAF50; -fx-stroke: #2E7D32; -fx-stroke-width: 2;");
        } else {
            // Offline status
            statusLabel.setText("Offline");
            statusLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-weight: normal; -fx-font-size: 12px;");
            statusIndicator.setStyle("-fx-fill: #9E9E9E; -fx-stroke: #757575; -fx-stroke-width: 2;");
        }
    }

    /**
     * Set typing status
     */
    public void setTyping(boolean typing) {
        this.isTyping = typing;
        if (typing) {
            typingIndicator.setVisible(true);
            typingAnimation.play();
        } else {
            typingIndicator.setVisible(false);
            typingAnimation.stop();
        }
    }

    /**
     * Initialize typing animation
     */
    private void initializeTypingAnimation() {
        typingAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> typingIndicator.setText("typing")),
            new KeyFrame(Duration.millis(500), e -> typingIndicator.setText("typing.")),
            new KeyFrame(Duration.millis(1000), e -> typingIndicator.setText("typing..")),
            new KeyFrame(Duration.millis(1500), e -> typingIndicator.setText("typing..."))
        );
        typingAnimation.setCycleCount(Animation.INDEFINITE);
    }

    public void setChatData(Long chatId, String lastMessageText, String lastMessageTime, int unreadCount) {
        this.chatId = chatId;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;

        updateDisplay();
    }

    private void updateDisplay() {
        if (lastMessageText != null && !lastMessageText.isEmpty()) {
            lastMessage.setText(lastMessageText);
            timestampLabel.setText(lastMessageTime);
        }

        updateUnreadCount();
    }

    /**
     * Update unread count with enhanced styling
     */
    private void updateUnreadCount() {
        if (unreadCount > 0) {
            unreadCountLabel.setText(String.valueOf(unreadCount));
            unreadCountLabel.setVisible(true);
            
            // Enhanced unread badge styling
            if (unreadCount > 99) {
                unreadCountLabel.setText("99+");
            }
            
            unreadCountLabel.setStyle("""
                -fx-background-color: #FF3B30;
                -fx-text-fill: white;
                -fx-background-radius: 12;
                -fx-padding: 4 8;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
                -fx-min-width: 20px;
                -fx-alignment: center;
                -fx-effect: dropshadow(gaussian, rgba(255, 59, 48, 0.4), 4, 0, 0, 1);
                """);
        } else {
            unreadCountLabel.setVisible(false);
        }
    }

    public void setUnreadCount(int count) {
        this.unreadCount = count;
        updateUnreadCount();
    }

    public void setLastMessage(String message, String time) {
        this.lastMessageText = message;
        this.lastMessageTime = time;
        updateDisplay();
    }

    /**
     * Update last seen time
     */
    public void setLastSeen(LocalDateTime lastSeen) {
        if (lastSeen != null) {
            String formattedTime = formatLastSeen(lastSeen);
            timestampLabel.setText("Last seen " + formattedTime);
            timestampLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        }
    }

    /**
     * Format last seen time
     */
    private String formatLastSeen(LocalDateTime lastSeen) {
        LocalDateTime now = LocalDateTime.now();
        
        if (lastSeen.toLocalDate().equals(now.toLocalDate())) {
            // Today - show time
            return "today at " + lastSeen.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (lastSeen.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            // Yesterday
            return "yesterday at " + lastSeen.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            // Other days
            return lastSeen.format(DateTimeFormatter.ofPattern("MMM dd"));
        }
    }

    /**
     * Set user status (away, busy, etc.)
     */
    public void setUserStatus(String status) {
        if (status != null) {
            switch (status.toLowerCase()) {
                case "away":
                    statusLabel.setText("Away");
                    statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 12px;");
                    statusIndicator.setStyle("-fx-fill: #FF9800; -fx-stroke: #F57C00; -fx-stroke-width: 2;");
                    break;
                case "busy":
                    statusLabel.setText("Busy");
                    statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 12px;");
                    statusIndicator.setStyle("-fx-fill: #F44336; -fx-stroke: #D32F2F; -fx-stroke-width: 2;");
                    break;
                case "online":
                    updateOnlineStatus(true);
                    break;
                default:
                    updateOnlineStatus(false);
                    break;
            }
        }
    }

    public Long getChatId() {
        return chatId;
    }

    public UserDTO getUser() {
        return user;
    }

    private void setupHoverEffects() {
        chatItem.setOnMouseEntered(event -> {
            chatItem.setStyle("""
                -fx-background-color: #4a5568;
                -fx-background-radius: 12px;
                -fx-border-color: #3498db;
                -fx-border-width: 1px;
                -fx-border-radius: 12px;
                -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 8, 0, 0, 2);
                """);
            chatItem.setCursor(javafx.scene.Cursor.HAND);
        });

        chatItem.setOnMouseExited(event -> {
            chatItem.setStyle("""
                -fx-background-color: #34495e;
                -fx-background-radius: 12px;
                -fx-border-color: #4a5568;
                -fx-border-width: 1px;
                -fx-border-radius: 12px;
                """);
            chatItem.setCursor(javafx.scene.Cursor.DEFAULT);
        });
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (typingAnimation != null) {
            typingAnimation.stop();
        }
    }
}
