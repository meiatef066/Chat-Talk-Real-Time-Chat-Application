package com.system.chattalkdesktop.common;


import com.system.chattalkdesktop.notificationPage.ApiNotification;
import com.system.chattalkdesktop.utils.NavigationUtil;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class sidebarController {
    @FXML private Label notificationCountLabel;
    private final ApiNotification apiNotification = ApiNotification.getInstance();

    public void initialize() {
        updateNotificationCount();
        // Set up periodic notification count updates
        setupNotificationCountUpdates();
    }
    
    /**
     * Setup periodic updates for notification count
     */
    private void setupNotificationCountUpdates() {
        // Update notification count every 30 seconds
        Thread updateThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // 30 seconds
                    updateNotificationCount();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    /**
     * Update notification count display
     */
    private void updateNotificationCount() {
        try {
            long unreadCount = apiNotification.getNotifications()
                    .stream()
                    .filter(notification -> !notification.isRead())
                    .count();
            Platform.runLater(() ->
                    notificationCountLabel.setText("notify : " + unreadCount)
            );
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    notificationCountLabel.setText("notify : 0")
            );
        }
    }
    
    /**
     * Manually refresh notification count (can be called from other parts of the app)
     */
    public void refreshNotificationCount() {
        updateNotificationCount();
    }
    
    @FXML
    public void navigateToProfile( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/ProfilePage/Profile.fxml","Profile");
    }
    
    @FXML
    public void navigateToChat( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/MainChat/ChatApp.fxml","ChatApp❤");
    }
    
    @FXML
    public void navigateToGroup( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/GroupPage/Group.fxml","Groups❤");
    }
    
    @FXML
    public void navigateToSearch( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/SearchPage/SearchForUsers.fxml","Find Friends 💌");
    }
    
    @FXML
    public void navigateToNotificationList( MouseEvent event ) {
        // Don't clear session - user should stay logged in to view notifications
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/notificationPage/Notification.fxml","Notifications 🎶");
    }
    
    @FXML
    public void logout( MouseEvent event ) {
        SessionManager.getInstance().clearSession();
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/auth/Login.fxml","login🎶");
    }
}
