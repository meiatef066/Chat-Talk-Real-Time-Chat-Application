package com.system.chattalkdesktop.MainChat;

import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Chat message item component with flexible sizing and read status
 */
@Getter
public class ChatMessageItem extends HBox {

    private MessageDTO message; // Removed final to allow updates
    private final Label messageLabel;
    private final Label senderLabel;
    private final MessageReadIndicator readIndicator;
    private final VBox messageContainer;
    private final boolean isOwnMessage;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MESSAGE_WIDTH = 300;
    private static final int MIN_MESSAGE_WIDTH = 100;

    public ChatMessageItem(MessageDTO message) {
        this.message = message;
        this.isOwnMessage = message.getSenderId().equals(SessionManager.getInstance().getCurrentUser().getId());

        // Create message label with flexible sizing
        messageLabel = new Label(message.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(MAX_MESSAGE_WIDTH);
        messageLabel.setMinWidth(MIN_MESSAGE_WIDTH);
        messageLabel.setPadding(new Insets(8, 12, 8, 12));
        messageLabel.setFont(Font.font(14));

        // Create sender label
        senderLabel = new Label();
        senderLabel.setFont(Font.font(10));
        senderLabel.setPadding(new Insets(2, 0, 2, 0));

        // Create read indicator
        readIndicator = new MessageReadIndicator();

        // Create message container
        messageContainer = new VBox(4);
        messageContainer.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.getChildren().addAll(senderLabel, messageLabel, readIndicator);

        // Setup main container
        setupContainer();
        setupStyling();
        setupMessageContent();
    }

    private void setupContainer() {
        setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(8);

        if (isOwnMessage) {
            getChildren().addAll(messageContainer);
        } else {
            getChildren().addAll(messageContainer);
        }
    }

    private void setupStyling() {
        if (isOwnMessage) {
            // Own message styling
            messageLabel.setStyle("""
                -fx-background-color: #007AFF;
                -fx-text-fill: white;
                -fx-background-radius: 18px;
                -fx-border-radius: 18px;
                -fx-font-weight: normal;
                """);
            senderLabel.setStyle("-fx-text-fill: #666666; -fx-font-weight: bold;");
            senderLabel.setText("You");
            senderLabel.setTextAlignment(TextAlignment.RIGHT);
        } else {
            // Other user's message styling
            messageLabel.setStyle("""
                -fx-background-color: #E9E9EB;
                -fx-text-fill: black;
                -fx-background-radius: 18px;
                -fx-border-radius: 18px;
                -fx-font-weight: normal;
                """);
            senderLabel.setStyle("-fx-text-fill: #666666; -fx-font-weight: bold;");
            senderLabel.setText("Friend"); // You can replace this with actual sender name
            senderLabel.setTextAlignment(TextAlignment.LEFT);
        }
    }

    private void setupMessageContent() {
        // Set message content
        messageLabel.setText(message.getContent());

        // Set timestamp
        String timestamp = formatTimestamp(message.getTimestamp());
        readIndicator.setTimestamp(timestamp);

        // Set read status
        readIndicator.updateStatus(message.getIsRead(), timestamp);

        // Adjust message width based on content length
        adjustMessageWidth();
    }

    private void adjustMessageWidth() {
        String content = message.getContent();
        int contentLength = content.length();

        // Calculate optimal width based on content
        double optimalWidth;
        if (contentLength < 20) {
            optimalWidth = Math.max(MIN_MESSAGE_WIDTH, contentLength * 8);
        } else if (contentLength < 50) {
            optimalWidth = Math.min(MAX_MESSAGE_WIDTH, contentLength * 7);
        } else {
            optimalWidth = MAX_MESSAGE_WIDTH;
        }

        messageLabel.setPrefWidth(optimalWidth);
        messageLabel.setMaxWidth(optimalWidth);
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(TIME_FORMATTER);
    }

    /**
     * Update the read status of the message
     */
    public void updateReadStatus(boolean isRead) {
        readIndicator.updateStatus(isRead, formatTimestamp(message.getTimestamp()));
    }

    /**
     * Update message content and status
     */
    public void updateMessage(MessageDTO updatedMessage) {
        this.message = updatedMessage;
        messageLabel.setText(updatedMessage.getContent());
        readIndicator.updateStatus(updatedMessage.getIsRead(), formatTimestamp(updatedMessage.getTimestamp()));
        adjustMessageWidth();
    }

    /**
     * Set the sender name for the message
     */
    public void setSenderName(String senderName) {
        if (!isOwnMessage) {
            senderLabel.setText(senderName);
        }
    }

    /**
     * Get the message ID
     */
    public Long getMessageId() {
        return message.getMessageId();
    }

    /**
     * Check if this is the current user's message
     */
    public boolean isOwnMessage() {
        return isOwnMessage;
    }

    /**
     * Check if this message matches another message by ID
     */
    public boolean matchesMessage(MessageDTO otherMessage) {
        return this.message.getMessageId().equals(otherMessage.getMessageId());
    }

    /**
     * Get the read indicator for external updates
     */
    public MessageReadIndicator getReadIndicator() {
        return readIndicator;
    }
}
