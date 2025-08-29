package com.system.chattalkdesktop.notificationPage;

import com.system.chattalkdesktop.Dto.entity.NotificationDTO;
import com.system.chattalkdesktop.service.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class NotificationController {
    @FXML private TextField searchField;
    @FXML private ListView<HBox> notificationList;
    @FXML private Button loadMoreButton;
    @FXML private VBox notificationContainer;
    
    private ObservableList<HBox> notificationItems;
    private final ApiNotification apiNotification = ApiNotification.getInstance();
    private final NotificationManager notificationManager = NotificationManager.getInstance();
    
    // Pagination variables
    private int currentPage = 0;
    private final int pageSize = 20;
    private boolean hasMoreNotifications = true;
    private boolean isLoading = false;

    public void initialize() {
        log.debug("NotificationController.initialize() called");
        notificationItems = FXCollections.observableArrayList();
        notificationList.setItems(notificationItems);
        
        // Setup load more button
        setupLoadMoreButton();
        
        // Load initial notifications
        loadNotifications();
        
        // Setup search functionality
        searchField.textProperty().addListener((obs, oldValue, newValue) -> filterNotifications(newValue));
        
        // Setup real-time updates
        setupRealTimeUpdates();
        
        // Set up periodic refresh
        setupPeriodicRefresh();
    }
    
    /**
     * Setup load more button functionality
     */
    private void setupLoadMoreButton() {
        if (loadMoreButton != null) {
            loadMoreButton.setOnAction(e -> loadMoreNotifications());
            loadMoreButton.setVisible(false); // Initially hidden
        }
    }
    
    /**
     * Setup real-time updates from NotificationManager
     */
    private void setupRealTimeUpdates() {
        // Connect to NotificationManager for real-time updates
        notificationManager.setNotificationController(this);
        
        // Enable debug notifications to see real-time updates
        notificationManager.setShowDebugNotifications(true);
    }
    
    /**
     * Add new notification in real-time
     * This method is called by NotificationManager when a new notification arrives
     */
    public void addNewNotification(NotificationDTO notification) {
        if (notification == null) {
            return;
        }
        
        log.debug("Adding new notification in real-time: {}", notification.getTitle());
        
        Platform.runLater(() -> {
            try {
                // Add notification at the top of the list
                addNotificationItem(notification, 0);
                
                // Update notification count in sidebar
                updateNotificationCount();
                
                log.debug("✅ New notification added to UI: {}", notification.getTitle());
            } catch (Exception e) {
                log.error("Error adding new notification: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Update notification count in sidebar
     */
    private void updateNotificationCount() {
        try {
            int count = apiNotification.getNotificationCount();
            // This would need to be connected to the sidebar controller
            log.debug("Updated notification count: {}", count);
        } catch (Exception e) {
            log.error("Error updating notification count: {}", e.getMessage());
        }
    }
    
    /**
     * Load more notifications (pagination)
     */
    private void loadMoreNotifications() {
        if (isLoading || !hasMoreNotifications) {
            return;
        }
        
        isLoading = true;
        loadMoreButton.setDisable(true);
        
        try {
            log.debug("Loading more notifications, page: {}", currentPage + 1);
            List<NotificationDTO> newNotifications = apiNotification.getNotifications(currentPage + 1, pageSize);
            
            if (newNotifications.isEmpty()) {
                hasMoreNotifications = false;
                loadMoreButton.setVisible(false);
            } else {
                currentPage++;
                
                Platform.runLater(() -> {
                    for (NotificationDTO notification : newNotifications) {
                        addNotificationItem(notification);
                    }
                    
                    log.debug("Loaded {} more notifications, total: {}", newNotifications.size(), notificationItems.size());
                });
            }
        } catch (Exception e) {
            log.error("Failed to load more notifications: {}", e.getMessage());
            Platform.runLater(() -> {
                showErrorAlert("Error", "Failed to load more notifications", e.getMessage());
            });
        } finally {
            isLoading = false;
            loadMoreButton.setDisable(false);
        }
    }
    
    /**
     * Setup periodic refresh of notifications
     */
    private void setupPeriodicRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Refresh every minute
                    Platform.runLater(this::refreshNotifications);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in periodic refresh: {}", e.getMessage());
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    /**
     * Refresh notifications (reload from first page)
     */
    @FXML
    private void refreshNotifications() {
        try {
            log.debug("Refreshing notifications...");
            currentPage = 0;
            hasMoreNotifications = true;
            loadNotifications();
        } catch (Exception e) {
            log.error("Failed to refresh notifications: {}", e.getMessage());
        }
    }

    private void loadNotifications() {
        try {
            log.debug("Loading notifications, page: {}", currentPage);
            List<NotificationDTO> notifications = apiNotification.getNotifications(currentPage, pageSize);
            log.debug("Loaded {} notifications", notifications.size());
            
            Platform.runLater(() -> {
                if (currentPage == 0) {
                    // Clear list only on first page load
                    notificationItems.clear();
                }
                
                for (NotificationDTO notification : notifications) {
                    addNotificationItem(notification);
                }
                
                // Show/hide load more button
                if (loadMoreButton != null) {
                    loadMoreButton.setVisible(notifications.size() >= pageSize);
                }
                
                log.debug("Displayed {} notification items", notificationItems.size());
            });
        } catch (Exception e) {
            log.error("Failed to load notifications: {}", e.getMessage());
            Platform.runLater(() -> {
                showErrorAlert("Error", "Failed to load notifications", e.getMessage());
            });
        }
    }

    private void addNotificationItem(NotificationDTO notification) {
        addNotificationItem(notification, -1); // Add at the end
    }
    
    private void addNotificationItem(NotificationDTO notification, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/chattalkdesktop/notificationPage/NotificationItem.fxml"));
            HBox item = loader.load();
            NotificationItem controller = loader.getController();
            controller.setData(notification, this);
            
            if (index >= 0) {
                notificationItems.add(index, item);
            } else {
                notificationItems.add(item);
            }
        } catch (IOException e) {
            log.error("Failed to create notification item: {}", e.getMessage());
        }
    }

    private void filterNotifications(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshNotifications();
            return;
        }
        
        log.debug("Filtering notifications with query: {}", query);
        notificationItems.removeIf(item -> {
            NotificationItem controller = (NotificationItem) item.getProperties().get("controller");
            if (controller == null) return true;
            
            String messageText = controller.getMessageLabel().getText().toLowerCase();
            String titleText = controller.getTitleLabel().getText().toLowerCase();
            String queryLower = query.toLowerCase();
            
            // Search in both title and message
            return !messageText.contains(queryLower) && !titleText.contains(queryLower);
        });
    }

    @FXML
    private void clearAllNotifications() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear All Notifications");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to clear all notifications?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    apiNotification.deleteAllNotifications();
                    notificationItems.clear();
                    currentPage = 0;
                    hasMoreNotifications = true;
                    log.debug("All notifications cleared");
                } catch (Exception e) {
                    log.error("Failed to clear notifications: {}", e.getMessage());
                    showErrorAlert("Error", "Failed to clear notifications", e.getMessage());
                }
            }
        });
    }

    public void deleteNotification(Long notificationId) {
        try {
            apiNotification.deleteNotification(notificationId);
            // Remove from UI
            notificationItems.removeIf(item -> {
                NotificationItem controller = (NotificationItem) item.getProperties().get("controller");
                return controller != null && controller.getNotification().getId().equals(notificationId);
            });
            log.debug("Notification deleted: {}", notificationId);
        } catch (Exception e) {
            log.error("Failed to delete notification: {}", e.getMessage());
            showErrorAlert("Error", "Failed to delete notification", e.getMessage());
        }
    }

    public void markNotificationAsRead(Long notificationId) {
        try {
            apiNotification.markAsRead(notificationId);
            // Update UI to show as read
            notificationItems.forEach(item -> {
                NotificationItem controller = (NotificationItem) item.getProperties().get("controller");
                if (controller != null && controller.getNotification().getId().equals(notificationId)) {
                    controller.markAsRead();
                }
            });
            log.debug("Notification marked as read: {}", notificationId);
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", e.getMessage());
            showErrorAlert("Error", "Failed to mark notification as read", e.getMessage());
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}