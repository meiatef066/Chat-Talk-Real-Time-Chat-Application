package com.system.chattalkdesktop.MainChat;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;


/**
 * Message read indicator component showing read status and timestamp
 */
public class MessageReadIndicator extends HBox {

    private final Label timestampLabel;
    private final Circle readIndicator;
    private final Circle deliveredIndicator;

    public MessageReadIndicator() {
        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(4);
        setPadding(new Insets(2, 0, 0, 0));

        // Create timestamp label
        timestampLabel = new Label();
        timestampLabel.setFont(Font.font(10));
        timestampLabel.setStyle("-fx-text-fill: #999999;");

        // Create read indicator (double check mark)
        readIndicator = new Circle(4);
        readIndicator.setStyle("-fx-fill: #007AFF;");
        readIndicator.setVisible(false);

        // Create delivered indicator (single check mark)
        deliveredIndicator = new Circle(4);
        deliveredIndicator.setStyle("-fx-fill: #999999;");
        deliveredIndicator.setVisible(false);

        getChildren().addAll(timestampLabel, deliveredIndicator, readIndicator);
    }

    /**
     * Update the read status and timestamp
     */
    public void updateStatus(boolean isRead, String timestamp) {
        timestampLabel.setText(timestamp);

        if (isRead) {
            deliveredIndicator.setVisible(true);
            readIndicator.setVisible(true);
            deliveredIndicator.setStyle("-fx-fill: #007AFF;");
            readIndicator.setStyle("-fx-fill: #007AFF;");
        } else {
            deliveredIndicator.setVisible(true);
            readIndicator.setVisible(false);
            deliveredIndicator.setStyle("-fx-fill: #999999;");
        }
    }

    /**
     * Set only the timestamp
     */
    public void setTimestamp(String timestamp) {
        timestampLabel.setText(timestamp);
    }

    /**
     * Show sending status
     */
    public void showSending() {
        deliveredIndicator.setVisible(false);
        readIndicator.setVisible(false);
        timestampLabel.setText("Sending...");
    }

    /**
     * Show sent status
     */
    public void showSent() {
        deliveredIndicator.setVisible(true);
        readIndicator.setVisible(false);
        deliveredIndicator.setStyle("-fx-fill: #999999;");
    }

    /**
     * Show delivered status
     */
    public void showDelivered() {
        deliveredIndicator.setVisible(true);
        readIndicator.setVisible(false);
        deliveredIndicator.setStyle("-fx-fill: #007AFF;");
    }

    /**
     * Show read status
     */
    public void showRead() {
        deliveredIndicator.setVisible(true);
        readIndicator.setVisible(true);
        deliveredIndicator.setStyle("-fx-fill: #007AFF;");
        readIndicator.setStyle("-fx-fill: #007AFF;");
    }
}
