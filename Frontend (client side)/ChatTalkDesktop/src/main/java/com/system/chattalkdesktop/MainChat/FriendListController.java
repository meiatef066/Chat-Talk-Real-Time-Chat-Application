package com.system.chattalkdesktop.MainChat;


import com.system.chattalkdesktop.Dto.PendingFriendRequestDto;
import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.MainChat.APIService.ApiChatService;
import com.system.chattalkdesktop.MainChat.APIService.ApiContactService;
import com.system.chattalkdesktop.MainChat.APIService.ChatServiceApi;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.service.NotificationManager;
import com.system.chattalkdesktop.utils.NotificationConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.system.chattalkdesktop.service.ChatManagerService;
import com.system.chattalkdesktop.event.ChatStatusEvent;

public class FriendListController {

    @FXML
    private ListView<UserDTO> friendList;
    @FXML
    private ListView<PendingFriendRequestDto> pendingRequestList;
    @FXML
    private TabPane contactTabs;
    @FXML
    private VBox rightPane;

    private ChatWindowController chatWindowController;
    private Node chatWindowNode;

    private final ObservableList<UserDTO> friends = FXCollections.observableArrayList();
    private final ObservableList<PendingFriendRequestDto> pendingRequests = FXCollections.observableArrayList();

    // Cache for chat data
    private final Map<String, ChatData> chatDataCache = new ConcurrentHashMap<>();

    private final ChatManagerService chatManager = ChatManagerService.getInstance();

    public void refreshRealtimeConnection( javafx.event.ActionEvent actionEvent ) {
        // Test WebSocket connection
        NotificationManager notificationManager = NotificationManager.getInstance();
        try {
            // Always attempt to connect and subscribe to notifications
            notificationManager.subscribeToNotifications();
            notificationManager.testConnection(); // Test basic connectivity
            System.out.println("🔌 websocket Reconnect ");
        } catch (Exception e) {
            System.err.println("⚠️ WebSocket connection failed: " + e.getMessage());
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "WebSocket Error",
                    "Failed to connect to notifications. Please try again later."
            );
        }
    }

    // Data class to hold chat information
    private static class ChatData {
        Long chatId;
        String lastMessage;
        String lastMessageTime;
        int unreadCount;
        Date lastActivity;

        ChatData(Long chatId, String lastMessage, String lastMessageTime, int unreadCount, Date lastActivity) {
            this.chatId = chatId;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
            this.lastActivity = lastActivity;
        }
    }

    @FXML
    public void initialize() {
        setupLists();
        setupTabs();
        loadChatWindow();
        loadData();

        // Connect to NotificationManager for real-time updates
        connectNotificationManager();
        initializeRealTimeFriendStatus();
    }

    /**
     * Initialize real-time friend status updates
     */
    @FXML
    private void initializeRealTimeFriendStatus() {
        if (chatManager == null) {
            System.err.println("ChatManagerService not available for friend status updates");
            return;
        }

        // Set up callback for user status changes
        chatManager.setUserStatusChangedCallback(this::handleFriendStatusChanged);

        System.out.println("Real-time friend status updates initialized");
    }

    /**
     * Handle friend status change (online/offline)
     */
    private void handleFriendStatusChanged(UserDTO user) {
        Platform.runLater(() -> {
            // Update friend status in the UI
            updateFriendStatus(user.getId(), user.getIsOnline());

            // Show notification for friends coming online
            if (user.getIsOnline()) {
                showFriendOnlineNotification(user);
            }
        });
    }

    /**
     * Update friend status in the UI
     */
    private void updateFriendStatus(Long userId, Boolean isOnline) {
        // Find the friend in the list and update their status
        for (UserDTO friend : friends) {
            if (friend.getId().equals(userId)) {
                // Update the friend's online status
                friend.setIsOnline(isOnline);
                
                // Update the UI on JavaFX thread
                Platform.runLater(() -> {
                    // Refresh the friend list to show updated status
                    updateFriendListDisplay();
                    
                    // Show notification for friends coming online
                    if (isOnline) {
                        showFriendOnlineNotification(friend);
                    }
                });
                
                System.out.println("Friend " + friend.getFirstName() + " status updated: " + (isOnline ? "Online" : "Offline"));
                break;
            }
        }
    }

    /**
     * Show notification when friend comes online
     */
    private void showFriendOnlineNotification(UserDTO user) {
        String friendName = user.getFirstName() + " " + user.getLastName();
        
        // Show desktop notification
        NotificationServiceImpl.getInstance().showInfoNotification(
            "Friend Online",
            friendName + " is now online!"
        );
        
        System.out.println("Friend " + friendName + " is now online!");
    }

    /**
     * Get user display name
     */
    private String getUserDisplayName(Long userId) {
        for (UserDTO friend : friends) {
            if (friend.getId().equals(userId)) {
                return friend.getFirstName() + " " + friend.getLastName();
            }
        }
        return "Friend " + userId;
    }

    /**
     * Handle typing indicator for a friend
     */
    public void handleFriendTyping(Long userId, boolean isTyping) {
        Platform.runLater(() -> {
            // Find the friend in the list and update typing status
            for (UserDTO friend : friends) {
                if (friend.getId().equals(userId)) {
                    // Update the UI to show typing indicator
                    updateTypingIndicator(friend, isTyping);
                    break;
                }
            }
        });
    }

    /**
     * Update typing indicator for a specific friend
     */
    private void updateTypingIndicator(UserDTO friend, boolean isTyping) {
        // This would need to be implemented based on your UI structure
        // For now, we'll just log the typing status
        System.out.println("Friend " + friend.getFirstName() + " is " + (isTyping ? "typing..." : "not typing"));
        
        // TODO: Update the specific friend's UI item to show/hide typing indicator
        // This would require accessing the ChatUserItemController for that specific friend
    }

    /**
     * Update friend's last seen time
     */
    public void updateFriendLastSeen(Long userId, java.time.LocalDateTime lastSeen) {
        Platform.runLater(() -> {
            // Find the friend in the list and update last seen
            for (UserDTO friend : friends) {
                if (friend.getId().equals(userId)) {
                    // Update the UI to show last seen time
                    updateLastSeenDisplay(friend, lastSeen);
                    break;
                }
            }
        });
    }

    /**
     * Update last seen display for a specific friend
     */
    private void updateLastSeenDisplay(UserDTO friend, java.time.LocalDateTime lastSeen) {
        // This would need to be implemented based on your UI structure
        System.out.println("Friend " + friend.getFirstName() + " last seen: " + lastSeen);
        
        // TODO: Update the specific friend's UI item to show last seen time
        // This would require accessing the ChatUserItemController for that specific friend
    }

    private void setupLists() {
        friendList.setItems(friends);
        pendingRequestList.setItems(pendingRequests);
        friendList.setCellFactory(this::createFriendCell);
        pendingRequestList.setCellFactory(this::createPendingRequestCell);

        friendList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                UserDTO selectedUser = friendList.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    openChatWith(selectedUser);
                }
            }
        });
    }

    private void setupTabs() {
        contactTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadData();
            }
        });
    }

    private void loadData() {
        loadFriends();
        loadPendingRequests();
    }

    private void loadFriends() {
        Task<List<UserDTO>> task = ApiContactService.getFriendList();
        task.setOnSucceeded(e -> {
            List<UserDTO> data = task.getValue();
            if (data != null) {
                Platform.runLater(() -> {
                    friends.setAll(data);
                    // Load chat data for each friend
                    loadChatDataForFriends(data);
                });
            }
        });
        task.setOnFailed(e -> {
            System.err.println("Failed to load friends: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void loadChatDataForFriends(List<UserDTO> friendList) {
        if (friendList == null || friendList.isEmpty()) {
            System.out.println("No friends to load chat data for");
            return;
        }

        System.out.println("Loading chat data for " + friendList.size() + " friends");

        for (UserDTO friend : friendList) {
            try {
                loadChatDataForFriend(friend);
            } catch (Exception e) {
                System.err.println("Error loading chat data for friend " + friend.getEmail() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadChatDataForFriend(UserDTO friend) {
        if (friend == null || friend.getEmail() == null) {
            System.err.println("Invalid friend data, skipping chat data loading");
            return;
        }

        // Get or create chat
        Task<Long> chatTask = ChatServiceApi.getOrCreatePrivateChat(friend.getEmail());
        chatTask.setOnSucceeded(e -> {
            try {
                Long chatId = chatTask.getValue();
                if (chatId != null) {
                    // Load unread count
                    loadUnreadCount(friend, chatId);
                    // Load last message
                    loadLastMessage(friend, chatId);
                }
            } catch (Exception ex) {
                System.err.println("Error processing chat data for friend " + friend.getEmail() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        chatTask.setOnFailed(e -> {
            System.err.println("Failed to get/create chat for friend " + friend.getEmail() + ": " + chatTask.getException().getMessage());
        });
        new Thread(chatTask).start();
    }

    private void loadUnreadCount(UserDTO friend, Long chatId) {
        try {
            Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
            if (currentUserId == null) {
                System.err.println("Warning: Current user ID is null, skipping unread count for friend: " + friend.getEmail());
                return;
            }

            Task<Integer> unreadTask = ApiChatService.getUnreadCount(chatId, currentUserId);
            unreadTask.setOnSucceeded(e -> {
                Integer unreadCount = unreadTask.getValue();
                if (unreadCount != null) {
                    updateFriendChatData(friend.getEmail(), chatId, null, null, unreadCount, null);
                }
            });
            unreadTask.setOnFailed(e -> {
                System.err.println("Failed to load unread count for friend " + friend.getEmail() + ": " + unreadTask.getException().getMessage());
            });
            new Thread(unreadTask).start();
        } catch (Exception e) {
            System.err.println("Error loading unread count for friend " + friend.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLastMessage(UserDTO friend, Long chatId) {
        Task<List<MessageDTO>> messageTask =
                ApiChatService.getChatHistory(chatId, 0, 1);
        messageTask.setOnSucceeded(e -> {
            List<MessageDTO> messages = messageTask.getValue();
            if (messages != null && !messages.isEmpty()) {
                MessageDTO lastMessage = messages.get(0);
                updateFriendChatData(friend.getEmail(), chatId, lastMessage.getContent(),
                        formatTimestamp(lastMessage.getTimestamp()), null, lastMessage.getTimestamp());
            }
        });
        new Thread(messageTask).start();
    }

    private void updateFriendChatData(String friendEmail, Long chatId, String lastMessage,
                                      String lastMessageTime, Integer unreadCount, java.time.LocalDateTime timestamp) {
        ChatData existingData = chatDataCache.get(friendEmail);
        if (existingData == null) {
            existingData = new ChatData(chatId, "", "", 0, new Date());
        }

        if (lastMessage != null) existingData.lastMessage = lastMessage;
        if (lastMessageTime != null) existingData.lastMessageTime = lastMessageTime;
        if (unreadCount != null) existingData.unreadCount = unreadCount;
        if (timestamp != null) existingData.lastActivity = Date.from(timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant());

        chatDataCache.put(friendEmail, existingData);

        // Update UI
        Platform.runLater(() -> updateFriendListDisplay());
    }

    private String formatTimestamp(java.time.LocalDateTime timestamp) {
        if (timestamp == null) return "";

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime today = now.toLocalDate().atStartOfDay();
        java.time.LocalDateTime yesterday = today.minusDays(1);

        if (timestamp.isAfter(today)) {
            return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else if (timestamp.isAfter(yesterday)) {
            return "Yesterday";
        } else {
            return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
        }
    }

    private void updateFriendListDisplay() {
        // Sort friends by last activity and unread count
        List<UserDTO> sortedFriends = new ArrayList<>(friends);
        sortedFriends.sort((f1, f2) -> {
            ChatData data1 = chatDataCache.get(f1.getEmail());
            ChatData data2 = chatDataCache.get(f2.getEmail());

            if (data1 == null && data2 == null) return 0;
            if (data1 == null) return 1;
            if (data2 == null) return -1;

            // First sort by unread count (descending)
            int unreadCompare = Integer.compare(data2.unreadCount, data1.unreadCount);
            if (unreadCompare != 0) return unreadCompare;

            // Then sort by last activity (descending)
            if (data1.lastActivity != null && data2.lastActivity != null) {
                return data2.lastActivity.compareTo(data1.lastActivity);
            }

            return 0;
        });

        friends.setAll(sortedFriends);
    }

    private void loadPendingRequests() {
        Task<List<PendingFriendRequestDto>> task = ApiContactService.getPendingRequests();
        task.setOnSucceeded(e -> {
            List<PendingFriendRequestDto> data = task.getValue();
            if (data != null) {
                Platform.runLater(() -> pendingRequests.setAll(data));
            }
        });
        new Thread(task).start();
    }

    private void connectNotificationManager() {
        try {
            NotificationManager notificationManager =
                    NotificationManager.getInstance();

            // Use configuration to set notification levels
            NotificationConfig.setMinimalNotifications();
            notificationManager.setShowConnectionNotifications(
                    NotificationConfig.isShowConnectionNotifications());
            notificationManager.setShowDebugNotifications(
                    NotificationConfig.isShowDebugNotifications());

            notificationManager.setFriendListController(this);
            notificationManager.subscribeToNotifications();
        } catch (Exception e) {
            System.err.println("Failed to connect notification manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadChatWindow() {
        try {
            System.out.println("Loading ChatWindowPane.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatWindowPane.fxml"));
            chatWindowNode = loader.load();
            chatWindowController = loader.getController();

            System.out.println("ChatWindowController loaded: " + (chatWindowController != null));

            if (chatWindowController != null) {
                // Set up the back button callback
                chatWindowController.setOnBackToFriendList(() -> {
                    // Return to friend list view
                    showFriendListView();
                });
                System.out.println("Back button callback set up successfully");
            }

            // Initially show the friend list view
            showFriendListView();

        } catch (IOException e) {
            System.err.println("Failed to load chat window: " + e.getMessage());
            e.printStackTrace();
            // Show error dialog or fallback
            showError("Failed to load chat window", e.getMessage());
        }
    }

    private void showFriendListView() {
        // Clear right pane and show friend list
        rightPane.getChildren().clear();

        // Create a simple welcome message or placeholder
        Label welcomeLabel = new Label("Select a friend to start chatting");
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-alignment: center;");
        welcomeLabel.setMaxWidth(Double.MAX_VALUE);
        welcomeLabel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(welcomeLabel, javafx.scene.layout.Priority.ALWAYS);

        rightPane.getChildren().add(welcomeLabel);
    }

    private void showChatWindow() {
        if (chatWindowNode != null) {
            rightPane.getChildren().clear();
            rightPane.getChildren().add(chatWindowNode);
        }
    }

    private ListCell<UserDTO> createFriendCell(ListView<UserDTO> listView) {
        return new ListCell<UserDTO>() {
            @Override
            protected void updateItem(UserDTO user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                try {
                    HBox cellContent = (HBox) loadFXML("ChatUserItem.fxml");
                    ChatUserItemController controller = (ChatUserItemController) cellContent.getUserData();

                    if (controller != null) {
                        controller.setUserData(user);

                        // Set chat data if available
                        ChatData chatData = chatDataCache.get(user.getEmail());
                        if (chatData != null) {
                            controller.setChatData(chatData.chatId, chatData.lastMessage,
                                    chatData.lastMessageTime, chatData.unreadCount);
                        }

                        setGraphic(cellContent);
                    } else {
                        setText(user.getFirstName() + " " + user.getLastName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setText(user.getFirstName() + " " + user.getLastName());
                }
            }
        };
    }

    private ListCell<PendingFriendRequestDto> createPendingRequestCell(ListView<PendingFriendRequestDto> listView) {
        return new ListCell<PendingFriendRequestDto>() {
            @Override
            protected void updateItem(PendingFriendRequestDto request, boolean empty) {
                super.updateItem(request, empty);

                if (empty || request == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                try {
                    HBox cellContent = (HBox) loadFXML("PendingRequestItem.fxml");
                    PendingItemController controller = (PendingItemController) cellContent.getUserData();

                    if (controller != null) {
                        controller.setRequestData(request);
                        controller.setOnRequestAction(FriendListController.this::refreshAll);
                        setGraphic(cellContent);
                    } else {
                        setText(request.getFirstName() + " " + request.getLastName());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    setText(request.getFirstName() + " " + request.getLastName());
                }
            }
        };
    }

    private Object loadFXML(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Object content = loader.load();
        Object controller = loader.getController();

        if (content instanceof javafx.scene.Node) {
            ((javafx.scene.Node) content).setUserData(controller);
        }

        return content;
    }

    private void openChatWith(UserDTO selectedUser) {
        System.out.println("openChatWith called for user: " + selectedUser.getFirstName());
        System.out.println("chatWindowController is null: " + (chatWindowController == null));

        if (chatWindowController != null && selectedUser != null) {
            System.out.println("Setting user data in ChatWindowController...");
            chatWindowController.setUserData(selectedUser);
            showChatWindow();

            // Mark messages as read when opening chat
            ChatData chatData = chatDataCache.get(selectedUser.getEmail());
            if (chatData != null && chatData.chatId != null) {
                try {
                    Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                    if (currentUserId != null) {
                        Task<Void> markReadTask = ApiChatService.markMessagesAsRead(chatData.chatId, currentUserId);
                        markReadTask.setOnSucceeded(e -> {
                            // Update unread count to 0
                            updateFriendChatData(selectedUser.getEmail(), chatData.chatId, null, null, 0, null);
                        });
                        markReadTask.setOnFailed(e -> {
                            System.err.println("Failed to mark messages as read: " + markReadTask.getException().getMessage());
                        });
                        new Thread(markReadTask).start();
                    } else {
                        System.err.println("Warning: Current user ID is null, cannot mark messages as read");
                    }
                } catch (Exception e) {
                    System.err.println("Error marking messages as read: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("ERROR: Cannot open chat - controller or user is null");
            showError("Error", "Unable to open chat. Please try again.");
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void refreshAll() {
        loadData();
    }

    public void updateUnreadCount(String friendEmail, int count) {
        ChatData chatData = chatDataCache.get(friendEmail);
        if (chatData != null) {
            updateFriendChatData(friendEmail, chatData.chatId, null, null, count, null);
        }
    }

    public void addNewMessage(String friendEmail, String messageContent, java.time.LocalDateTime timestamp) {
        updateFriendChatData(friendEmail, null, messageContent, formatTimestamp(timestamp), null, timestamp);
    }

    /**
     * Handle new message from WebSocket notification
     * This method is called by NotificationManager when a new message arrives
     */
    public void handleNewMessage(MessageDTO message) {
        if (message == null) {
            return;
        }

        // Find the friend by sender ID
        Long senderId = message.getSenderId();
        Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        
        if (senderId == null || currentUserId == null || senderId.equals(currentUserId)) {
            return; // Don't handle own messages
        }

        // Find the friend in our list
        UserDTO sender = friends.stream()
                .filter(friend -> friend.getId().equals(senderId))
                .findFirst()
                .orElse(null);

        if (sender != null) {
            // Update chat data with new message
            updateFriendChatData(
                sender.getEmail(), 
                message.getChatId(), 
                message.getContent(), 
                formatTimestamp(message.getTimestamp()), 
                null, // Keep existing unread count for now
                message.getTimestamp()
            );
            
            System.out.println("✅ Updated chat data for friend: " + sender.getFirstName() + " with new message");
        } else {
            System.out.println("⚠️ Could not find friend with ID: " + senderId + " for new message");
        }
    }

}