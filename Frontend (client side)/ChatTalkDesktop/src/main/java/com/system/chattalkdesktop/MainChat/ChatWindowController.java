package com.system.chattalkdesktop.MainChat;


import com.system.chattalkdesktop.Dto.ChatDto.SendMessageRequest;
import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.MainChat.APIService.ApiChatService;
import com.system.chattalkdesktop.MainChat.APIService.ChatServiceApi;
import com.system.chattalkdesktop.service.ChatMessageObserver;
import com.system.chattalkdesktop.service.MessageUpdateListener;
import com.system.chattalkdesktop.service.NotificationManager;
import com.system.chattalkdesktop.service.RealTimeChatService;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the chat window
 * Implements MessageUpdateListener for real-time message updates
 */
@Slf4j
public class ChatWindowController implements MessageUpdateListener {
    @FXML
    private Label friendNameLabel;
    @FXML
    private Label userEmail;
//    @FXML
//    private Label lastMessage;
    @FXML
    private ImageView avater;
    @FXML
    private Label status;
    @FXML
    private Button backButton;
    @FXML
    private Button sendButton;
    @FXML
    private TextArea messageInput;
    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane chatScrollPane;

    // Connection status indicator
    private ConnectionStatusIndicator connectionIndicator;
    private HBox headerContainer;

    @Getter
    private UserDTO currentUser;
    private Long currentChatId;
    @Setter
    private Runnable onBackToFriendList;
    private final ObservableList<ChatMessageItem> messages = FXCollections.observableArrayList();
    private final NotificationManager notificationManager = NotificationManager.getInstance();
    private final ChatMessageObserver messageObserver = ChatMessageObserver.getInstance();
    private final RealTimeChatService realTimeChatService = RealTimeChatService.getInstance();
    
    // Message count tracking
    private int totalMessageCount = 0;
    private int unreadMessageCount = 0;

    @FXML
    public void initialize() {
        log.debug("ChatWindowController.initialize() called");
        log.debug("chatContainer is null: {}", (chatContainer == null));
        log.debug("chatScrollPane is null: {}", (chatScrollPane == null));
        log.debug("friendNameLabel is null: {}", (friendNameLabel == null));
        log.debug("messageInput is null: {}", (messageInput == null));

        // Subscribe to message updates
        messageObserver.addListener(this);

        // Setup send button action
        if (sendButton != null) {
            sendButton.setOnAction(this::sendMessageButton);
        }

        // Setup connection status indicator
        setupConnectionIndicator();

        // Monitor connection status
        monitorConnectionStatus();
    }

    private void setupConnectionIndicator() {
        // Create connection status indicator
        connectionIndicator = new ConnectionStatusIndicator();

        // Add to header if available
        if (friendNameLabel != null && friendNameLabel.getParent() instanceof HBox) {
            headerContainer = (HBox) friendNameLabel.getParent();
            headerContainer.getChildren().add(connectionIndicator);
        }
    }

    private void monitorConnectionStatus() {
        // Monitor WebSocket connection status
        Task<Void> connectionMonitor = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (!isCancelled()) {
                    try {
                        boolean isConnected = notificationManager.isConnected();
                        Platform.runLater(() -> {
                            if (isConnected) {
                                connectionIndicator.setConnected();
                            } else {
                                connectionIndicator.setDisconnected();
                                log.warn("WebSocket connection lost");
                            }
                        });
                        Thread.sleep(2000); // Check every 2 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error monitoring connection: {}", e.getMessage());
                        Platform.runLater(() -> {
                            connectionIndicator.setError();
                            log.error("Connection monitoring failed: {}", e.getMessage());
                        });
                        Thread.sleep(5000); // Wait longer on error
                    }
                }
                return null;
            }
        };

        Thread monitorThread = new Thread(connectionMonitor);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void setUserData(UserDTO user) {
        log.debug("ChatWindowController.setUserData called for user: {}", user.getFirstName());
        log.debug("chatContainer is null: {}", (chatContainer == null));
        log.debug("chatScrollPane is null: {}", (chatScrollPane == null));

        this.currentUser = user;
        this.friendNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        this.userEmail.setText(user.getEmail());
        this.status.setText(user.getIsOnline() != null && user.getIsOnline() ? "Online 🟢" : "Offline 🔴");
        if (user.getProfilePictureUrl() != null) {
            avater.setImage(new Image(user.getProfilePictureUrl()));
        }

        // Set up the back button
        setupBackButton();

        // Get or create chat and load messages
        getOrCreateChat();
        
        // Clear unread count for this user when chat is opened
        clearUnreadCountForUser(user.getEmail());
    }

    private void getOrCreateChat() {
        Task<Long> chatTask = ChatServiceApi.getOrCreatePrivateChat(currentUser.getEmail());
        chatTask.setOnSucceeded(e -> {
            Long chatId = chatTask.getValue();
            if (chatId != null) {
                this.currentChatId = chatId;
                loadChatHistory();
                // Mark messages as read when opening chat
                markMessagesAsRead();
                
                // Start real-time updates for this chat
                startRealTimeUpdates();
            }
        });
        chatTask.setOnFailed(e -> {
            log.error("Failed to get/create chat: {}", chatTask.getException().getMessage());
        });
        new Thread(chatTask).start();
    }

    /**
     * Start real-time updates for the current chat
     */
    private void startRealTimeUpdates() {
        if (currentChatId != null) {
            log.debug("Starting real-time updates for chat: {}", currentChatId);
            realTimeChatService.startRealTimeUpdates(currentChatId);
        }
    }

    /**
     * Stop real-time updates for the current chat
     */
    private void stopRealTimeUpdates() {
        if (currentChatId != null) {
            log.debug("Stopping real-time updates for chat: {}", currentChatId);
            realTimeChatService.stopRealTimeUpdates();
        }
    }

    private void loadChatHistory() {
        if (currentChatId == null) return;

        log.debug("Loading chat history for chat ID: {}", currentChatId);

        Task<List<MessageDTO>> historyTask = ApiChatService.getChatHistory(currentChatId, 0, 50);
        historyTask.setOnSucceeded(e -> {
            List<MessageDTO> messageList = historyTask.getValue();
            log.debug("Chat history loaded, message count: {}", (messageList != null ? messageList.size() : 0));

            if (messageList != null) {
                Platform.runLater(() -> {
                    messages.clear();
                    
                    // Backend returns messages in DESC order (newest first), so we need to reverse for UI
                    // to show oldest messages first (chronological order)
                    List<MessageDTO> sortedMessages = new ArrayList<>(messageList);
                    // Reverse the list to get chronological order (oldest first)
                    java.util.Collections.reverse(sortedMessages);
                    
                    log.debug("Sorted {} messages by timestamp", sortedMessages.size());
                    
                    for (MessageDTO message : sortedMessages) {
                        log.debug("Creating ChatMessageItem for message: {} at {}", 
                                message.getContent(), 
                                message.getTimestamp());
                        ChatMessageItem messageItem = new ChatMessageItem(message);
                        // Set sender name for incoming messages
                        if (!messageItem.isOwnMessage()) {
                            messageItem.setSenderName(currentUser.getFirstName());
                        }
                        messages.add(messageItem);
                    }
                    log.debug("Messages added to list, count: {}", messages.size());
                    updateChatDisplay();
                });
            }
        });
        historyTask.setOnFailed(e -> {
            log.error("Failed to load chat history: {}", historyTask.getException().getMessage());
        });
        new Thread(historyTask).start();
    }

    private void updateChatDisplay() {
        if (chatContainer == null) {
            log.error("ERROR: chatContainer is null! FXML injection failed.");
            return;
        }

        // Ensure messages are in chronological order before displaying
        sortMessagesByTimestamp();

        chatContainer.getChildren().clear();
        chatContainer.getChildren().addAll(messages);

        // Scroll to bottom
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    /**
     * Sort messages by timestamp to ensure proper chronological order
     */
    private void sortMessagesByTimestamp() {
        // Convert to list, sort, and update the observable list
        List<ChatMessageItem> sortedMessages = messages.stream()
                .sorted((m1, m2) -> {
                    LocalDateTime t1 = m1.getMessage().getTimestamp();
                    LocalDateTime t2 = m2.getMessage().getTimestamp();
                    
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return -1;
                    if (t2 == null) return 1;
                    return t1.compareTo(t2);
                })
                .toList();
        
        // Update the observable list with sorted messages
        messages.clear();
        messages.addAll(sortedMessages);
        
        log.debug("Messages sorted by timestamp, count: {}", messages.size());
    }

    private void markMessagesAsRead() {
        if (currentChatId == null) return;

        Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        if (currentUserId != null) {
            Task<Void> markReadTask = ApiChatService.markMessagesAsRead(currentChatId, currentUserId);
            markReadTask.setOnSucceeded(e -> {
                log.debug("Messages marked as read for chat: {}", currentChatId);
                // Update read status for all messages
                Platform.runLater(() -> {
                    messages.forEach(item -> {
                        if (!item.isOwnMessage()) {
                            item.updateReadStatus(true);
                        }
                    });
                });
            });
            new Thread(markReadTask).start();
        }
    }

    private void setupBackButton() {
        backButton.setOnAction(event -> {
            if (onBackToFriendList != null) {
                onBackToFriendList.run();
            }
        });
    }

    @FXML
    public void sendMessageButton( ActionEvent action) {
        sendMessageButton();
    }

    private void sendMessageButton() {
        String messageText = messageInput.getText().trim();
        log.debug("Send button clicked. Message: '{}', ChatId: {}", messageText, currentChatId);
        
        if (messageText.isEmpty()) {
            log.warn("Cannot send empty message");
            return;
        }
        
        if (currentChatId == null) {
            log.error("Cannot send message: currentChatId is null");
            return;
        }
        
        if (currentUser == null) {
            log.error("Cannot send message: currentUser is null");
            return;
        }
            // Create temporary message item with "sending" status
            MessageDTO tempMessage = MessageDTO.builder()
                    .messageId(-1L) // Temporary ID
                    .content(messageText)
                    .senderId(SessionManager.getInstance().getCurrentUser().getId())
                    .chatId(currentChatId)
                    .timestamp(LocalDateTime.now())
                    .isRead(false)
                    .build();

            ChatMessageItem tempItem = new ChatMessageItem(tempMessage);
            tempItem.getReadIndicator().showSending();

            Platform.runLater(() -> {
                messages.add(tempItem);
                updateChatDisplay();
                messageInput.clear();
            });

            // Create message request
            SendMessageRequest messageRequest = SendMessageRequest.builder()
                    .content(messageText)
                    .senderId(SessionManager.getInstance().getCurrentUser().getId())
                    .MessageType("TEXT")
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            Task<MessageDTO> sendTask = getMessageDTOTask(messageRequest, tempItem, messageText);
            new Thread(sendTask).start();
    }

    private Task<MessageDTO> getMessageDTOTask( SendMessageRequest messageRequest, ChatMessageItem tempItem, String messageText ) {
        Task<MessageDTO> sendTask = ApiChatService.sendMessage(currentChatId, messageRequest);
        sendTask.setOnSucceeded(e -> {
            MessageDTO sentMessage = sendTask.getValue();
            if (sentMessage != null) {
                            Platform.runLater(() -> {
                // Remove temporary message
                messages.remove(tempItem);

                // Add real message at the end (newest message)
                ChatMessageItem realItem = new ChatMessageItem(sentMessage);
                messages.add(realItem);
                updateChatDisplay();

                // Update last message
//                    lastMessage.setText(messageText !=null? messageText :"");

                // Notify observers
                messageObserver.notifyMessageSent(sentMessage);
            });
            }
        });
        sendTask.setOnFailed(e -> {
            log.error("Failed to send message: {}", sendTask.getException().getMessage());
            Platform.runLater(() -> {
                // Remove temporary message on failure
                messages.remove(tempItem);
                updateChatDisplay();
                
                // Show error notification to user
                com.system.chattalkdesktop.NotificationService.NotificationServiceImpl.getInstance()
                    .showErrorNotification(
                        "Failed to send message", 
                        "Error: " + sendTask.getException().getMessage()
                    );
            });
        });
        return sendTask;
    }

    // Implementation of MessageUpdateListener interface

    @Override
    public void onNewMessage(MessageDTO message) {
        log.debug("onNewMessage called for chat: {}, current chat: {}", message.getChatId(), currentChatId);
        log.debug("Message details - ID: {}, Content: {}, Sender: {}", 
                message.getMessageId(), message.getContent(), message.getSenderId());

        // Only handle messages for the current chat
        if (currentChatId != null && currentChatId.equals(message.getChatId())) {
            Platform.runLater(() -> {
                // Check if message is not from current user
                Long currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                if (currentUserId != null && !message.getSenderId().equals(currentUserId)) {
                    log.debug("Adding new message from other user: {} at {}", 
                            message.getContent(), message.getTimestamp());

                    // Check if message already exists to prevent duplicates
                    boolean messageExists = messages.stream()
                            .anyMatch(item -> item.getMessage().getMessageId().equals(message.getMessageId()));

                    if (!messageExists) {
                        ChatMessageItem messageItem = new ChatMessageItem(message);
                        messageItem.setSenderName(currentUser.getFirstName());
                        
                        // Add message at the end (newest message)
                        messages.add(messageItem);
                        updateChatDisplay();

                        // Mark messages as read since user is viewing the chat
                        markMessagesAsRead();
                        
                        log.debug("✅ New message added to chat UI: {}", message.getContent());
                    } else {
                        log.debug("Message already exists in chat: {}", message.getMessageId());
                    }
                } else {
                    log.debug("Skipping own message or null currentUserId: senderId={}, currentUserId={}", 
                            message.getSenderId(), currentUserId);
                }
            });
        } else {
            log.debug("Message not for current chat: messageChatId={}, currentChatId={}", 
                    message.getChatId(), currentChatId);
        }
    }

    @Override
    public void onMessageRead(Long chatId, Long userId) {
        // Handle message read events if needed
        if (currentChatId != null && currentChatId.equals(chatId)) {
            log.debug("Messages marked as read in current chat by user: {}", userId);
            // Update read status for own messages
            Platform.runLater(() -> {
                messages.forEach(item -> {
                    if (item.isOwnMessage()) {
                        item.updateReadStatus(true);
                    }
                });
            });
        }
    }

    @Override
    public void onMessageSent(MessageDTO message) {
        // Handle message sent events if needed
        if (currentChatId != null && currentChatId.equals(message.getChatId())) {
            log.debug("Message sent in current chat: {} at {}", message.getMessageId(), message.getTimestamp());

            // Don't add the message again if it's already in the list
            boolean messageExists = messages.stream()
                    .anyMatch(item -> item.getMessage().getMessageId().equals(message.getMessageId()));

            if (!messageExists) {
                Platform.runLater(() -> {
                    ChatMessageItem messageItem = new ChatMessageItem(message);
                    // Add sent message at the end (newest message)
                    messages.add(messageItem);
                    updateChatDisplay();
                });
            }
        }
    }

    @FXML
    public void onBackButtonClick(ActionEvent actionEvent) {
        // Stop real-time updates before navigating back
        stopRealTimeUpdates();
        
        if (onBackToFriendList != null) {
            onBackToFriendList.run();
        }
    }

    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (messageObserver != null) {
            messageObserver.removeListener(this);
        }
        if (connectionIndicator != null) {
            connectionIndicator.cleanup();
        }
        // Stop real-time updates
        stopRealTimeUpdates();
    }

    /**
     * Update the message count display in the UI
     */
    private void updateMessageCountDisplay() {
        Platform.runLater(() -> {
            // Update status label to show message count
            if (status != null) {
                String statusText = currentUser.getIsOnline() != null && currentUser.getIsOnline() ? 
                    "Online 🟢" : "Offline 🔴";
                if (unreadMessageCount > 0) {
                    statusText += " • " + unreadMessageCount + " unread";
                }
                status.setText(statusText);
            }
            
            // You can add more UI updates here for message counts
            log.debug("Message count display updated - Total: {}, Unread: {}", totalMessageCount, unreadMessageCount);
        });
    }
    
    /**
     * Test method to verify current functionality
     */
    public void testCurrentFunctionality() {
        log.info("=== Testing Current Chat Functionality ===");
        
        if (currentChatId != null) {
            log.info("Current Chat ID: {}", currentChatId);
            log.info("Total Messages: {}", totalMessageCount);
            log.info("Unread Messages: {}", unreadMessageCount);
            
            // Test marking messages as read
            markMessagesAsRead();
            
            // Test getting unread count
            if (SessionManager.getInstance().getCurrentUser() != null) {
                Task<Integer> unreadTask = ApiChatService.getUnreadCount(currentChatId, 
                    SessionManager.getInstance().getCurrentUser().getId());
                unreadTask.setOnSucceeded(e -> {
                    Integer count = unreadTask.getValue();
                    log.info("Current unread count from API: {}", count);
                });
                new Thread(unreadTask).start();
            }
        } else {
            log.warn("No current chat ID available for testing");
        }
    }
    @FXML
    public void sendMessage( KeyEvent keyEvent ) {
        sendMessageButton();
    }
    
    /**
     * Test method to verify send button functionality
     */
    public void testSendButton() {
        log.info("=== Testing Send Button Functionality ===");
        log.info("Current Chat ID: {}", currentChatId);
        log.info("Current User: {}", currentUser != null ? currentUser.getEmail() : "null");
        log.info("Message Input: {}", messageInput != null ? "available" : "null");
        log.info("Send Button: {}", sendButton != null ? "available" : "null");
        
        if (currentChatId == null) {
            log.error("❌ Cannot test send button: currentChatId is null");
            return;
        }
        
        if (currentUser == null) {
            log.error("❌ Cannot test send button: currentUser is null");
            return;
        }
        
        if (messageInput == null) {
            log.error("❌ Cannot test send button: messageInput is null");
            return;
        }
        
        log.info("✅ Send button test passed - all required components are available");
    }
    
    /**
     * Clear unread count for a user when chat is opened
     */
    private void clearUnreadCountForUser(String userEmail) {
        try {
            // Get the FriendListController reference from the notification manager
            com.system.chattalkdesktop.service.NotificationManager notificationManager = 
                com.system.chattalkdesktop.service.NotificationManager.getInstance();
            
            // Use reflection to get the FriendListController
            java.lang.reflect.Field field = notificationManager.getClass().getDeclaredField("friendListController");
            field.setAccessible(true);
            Object friendListController = field.get(notificationManager);
            
            if (friendListController != null) {
                // Call the clearUnreadCount method
                java.lang.reflect.Method method = friendListController.getClass().getMethod("clearUnreadCount", String.class);
                method.invoke(friendListController, userEmail);
                log.debug("✅ Cleared unread count for user: {}", userEmail);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not clear unread count for user {}: {}", userEmail, e.getMessage());
        }
    }
}
