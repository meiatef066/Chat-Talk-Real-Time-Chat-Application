package com.system.chattalkdesktop.MainChat;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Connection status indicator showing WebSocket connection state
 */
public class ConnectionStatusIndicator extends HBox {

    private final Circle statusDot;
    private final Label statusLabel;
    private final Timeline pulseAnimation;

    public enum ConnectionStatus {
        CONNECTED("Connected", "#4CAF50"),
        CONNECTING("Connecting...", "#FF9800"),
        DISCONNECTED("Disconnected", "#F44336"),
        ERROR("Connection Error", "#F44336");

        private final String text;
        private final String color;

        ConnectionStatus(String text, String color) {
            this.text = text;
            this.color = color;
        }

        public String getText() { return text; }
        public String getColor() { return color; }
    }

    public ConnectionStatusIndicator() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(4, 8, 4, 8));

        // Create status dot
        statusDot = new Circle(4);
        statusDot.setStyle("-fx-fill: #999999;");

        // Create status label
        statusLabel = new Label("Disconnected");
        statusLabel.setFont(Font.font(12));
        statusLabel.setStyle("-fx-text-fill: #666666;");

        getChildren().addAll(statusDot, statusLabel);

        // Create pulse animation for connecting state
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> statusDot.setOpacity(1.0)),
                new KeyFrame(Duration.millis(500), e -> statusDot.setOpacity(0.3)),
                new KeyFrame(Duration.millis(1000), e -> statusDot.setOpacity(1.0))
        );
        pulseAnimation.setCycleCount(Animation.INDEFINITE);

        // Set initial state
        setStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Set the connection status
     */
    public void setStatus(ConnectionStatus status) {
        statusLabel.setText(status.getText());
        statusDot.setStyle("-fx-fill: " + status.getColor() + ";");

        // Handle animations
        switch (status) {
            case CONNECTING:
                pulseAnimation.play();
                break;
            case CONNECTED:
                pulseAnimation.stop();
                statusDot.setOpacity(1.0);
                break;
            case DISCONNECTED:
            case ERROR:
                pulseAnimation.stop();
                statusDot.setOpacity(1.0);
                break;
        }
    }

    /**
     * Set connected status
     */
    public void setConnected() {
        setStatus(ConnectionStatus.CONNECTED);
    }

    /**
     * Set connecting status
     */
    public void setConnecting() {
        setStatus(ConnectionStatus.CONNECTING);
    }

    /**
     * Set disconnected status
     */
    public void setDisconnected() {
        setStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Set error status
     */
    public void setError() {
        setStatus(ConnectionStatus.ERROR);
    }

    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return statusLabel.getText().equals(ConnectionStatus.CONNECTED.getText());
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }
    }
}
