package com.system.chattalkdesktop.SearchService;

import com.system.chattalkdesktop.Dto.AuthDto.FriendRequestResponse;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.service.PerformanceOptimizationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimized search item controller with improved error handling and user experience
 */
@Slf4j
public class SearchItemController {

    @FXML
    public Label userName;
    @FXML
    public ImageView profileImage;
    @FXML
    public Label userEmail;
    @FXML
    public Button addButton;

    private SearchUserResultDTO user;
    private final AtomicBoolean isProcessingRequest = new AtomicBoolean(false);
    private final PerformanceOptimizationService performanceService = PerformanceOptimizationService.getInstance();

    /**
     * Set user data and configure UI accordingly
     */
    public void setUserData(SearchUserResultDTO userDTO) {
        if (userDTO == null) {
            log.warn("Attempted to set null user data");
            return;
        }

        this.user = userDTO;
        
        // Set basic user information
        userEmail.setText(userDTO.getEmail() != null ? userDTO.getEmail() : "No email");
        
        String fullName = buildFullName(userDTO);
        userName.setText(fullName);

        // Load profile image if available
        loadProfileImage(userDTO.getProfilePictureUrl());

        // Configure button based on relationship status
        configureButtonByStatus(userDTO.getRelationshipStatus());
    }

    /**
     * Build full name from user data
     */
    private String buildFullName(SearchUserResultDTO userDTO) {
        StringBuilder nameBuilder = new StringBuilder();
        
        if (userDTO.getFirstName() != null && !userDTO.getFirstName().trim().isEmpty()) {
            nameBuilder.append(userDTO.getFirstName().trim());
        }
        
        if (userDTO.getLastName() != null && !userDTO.getLastName().trim().isEmpty()) {
            if (nameBuilder.length() > 0) {
                nameBuilder.append(" ");
            }
            nameBuilder.append(userDTO.getLastName().trim());
        }
        
        // Fallback to username if no name is available
        if (nameBuilder.length() == 0) {
            if (userDTO.getUsername() != null && !userDTO.getUsername().trim().isEmpty()) {
                nameBuilder.append(userDTO.getUsername().trim());
            } else {
                nameBuilder.append("Unknown User");
            }
        }
        
        return nameBuilder.toString();
    }

    /**
     * Load profile image with error handling
     */
    private void loadProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            // Set default image or clear current image
            profileImage.setImage(null);
            return;
        }

        try {
            Image image = new Image(imageUrl, true); // true for background loading
            
            // Add listener for image loading completion
            image.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    log.debug("Failed to load profile image: {}", imageUrl);
                    // Could set a default image here
                    profileImage.setImage(null);
                }
            });
            
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1.0) {
                    log.debug("Profile image loaded successfully: {}", imageUrl);
                }
            });
            
            profileImage.setImage(image);
            
        } catch (Exception e) {
            log.error("Error loading profile image: {}", e.getMessage(), e);
            profileImage.setImage(null);
        }
    }

    /**
     * Configure button appearance and state based on relationship status
     */
    private void configureButtonByStatus(String status) {
        if (status == null) {
            status = "NONE";
        }

        switch (status.toUpperCase()) {
            case "PENDING":
                setButtonState(true, "Pending Request", "#595959", "white");
                break;
            case "ACCEPTED":
                setButtonState(true, "Already Friends", "#64fa7d", "white");
                break;
            case "BLOCKED":
                setButtonState(true, "Blocked", "#f45050", "white");
                break;
            case "NONE":
            default:
                setButtonState(false, "Add Contact", "", "");
                break;
        }
    }

    /**
     * Set button state with consistent styling
     */
    private void setButtonState(boolean disabled, String text, String backgroundColor, String textColor) {
        addButton.setDisable(disabled);
        addButton.setText(text);
        
        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            addButton.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", backgroundColor, textColor));
        } else {
            addButton.setStyle("");
        }
    }

    /**
     * Handle add user button click with improved error handling
     */
    @FXML
    public void addUser(ActionEvent actionEvent) {
        if (user == null) {
            log.warn("Cannot add user: user data is null");
            NotificationServiceImpl.getInstance().showErrorNotification("Error", "Invalid user data");
            return;
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("Cannot add user: email is null or empty");
            NotificationServiceImpl.getInstance().showErrorNotification("Error", "User email is not available");
            return;
        }

        // Prevent multiple simultaneous requests
        if (!isProcessingRequest.compareAndSet(false, true)) {
            log.debug("Friend request already in progress for user: {}", user.getEmail());
            return;
        }

        // Update UI to show processing state
        Platform.runLater(() -> {
            setButtonState(true, "Sending...", "#595959", "white");
        });

        // Create and execute friend request task
        Task<FriendRequestResponse> friendRequestTask = ApiSearchUsers.sendFriendRequest(user.getEmail());
        
        friendRequestTask.setOnSucceeded(e -> {
            try {
                FriendRequestResponse response = friendRequestTask.getValue();
                if (response != null) {
                    handleFriendRequestSuccess();
                } else {
                    handleFriendRequestFailure("No response received");
                }
            } catch (Exception ex) {
                log.error("Error processing friend request response: {}", ex.getMessage(), ex);
                handleFriendRequestFailure("Error processing response");
            } finally {
                isProcessingRequest.set(false);
            }
        });

        friendRequestTask.setOnFailed(e -> {
            Throwable exception = friendRequestTask.getException();
            log.error("Friend request failed for user {}: {}", user.getEmail(), exception.getMessage(), exception);
            handleFriendRequestFailure("Request failed: " + exception.getMessage());
            isProcessingRequest.set(false);
        });

        friendRequestTask.setOnCancelled(e -> {
            log.debug("Friend request cancelled for user: {}", user.getEmail());
            handleFriendRequestCancelled();
            isProcessingRequest.set(false);
        });

        // Execute the task using performance optimization service
        performanceService.executeBackgroundTask(() -> friendRequestTask.run());
    }

    /**
     * Handle successful friend request
     */
    private void handleFriendRequestSuccess() {
        Platform.runLater(() -> {
            setButtonState(true, "Request Sent", "#595959", "white");
            NotificationServiceImpl.getInstance().showSuccessNotification(
                "Success", 
                "Friend request sent successfully to " + user.getEmail() + " 💌🎶"
            );
            
            // Update the user's relationship status
            if (user != null) {
                user.setRelationshipStatus("PENDING");
            }
        });
    }

    /**
     * Handle failed friend request
     */
    private void handleFriendRequestFailure(String errorMessage) {
        Platform.runLater(() -> {
            setButtonState(false, "Add Contact", "", "");
            NotificationServiceImpl.getInstance().showErrorNotification(
                "Request Failed", 
                "Failed to send friend request: " + errorMessage
            );
        });
    }

    /**
     * Handle cancelled friend request
     */
    private void handleFriendRequestCancelled() {
        Platform.runLater(() -> {
            setButtonState(false, "Add Contact", "", "");
            log.debug("Friend request was cancelled for user: {}", user.getEmail());
        });
    }

    /**
     * Get the current user data
     */
    public SearchUserResultDTO getUser() {
        return user;
    }

    /**
     * Check if a request is currently being processed
     */
    public boolean isProcessingRequest() {
        return isProcessingRequest.get();
    }

    /**
     * Cleanup method for proper resource management
     */
    public void cleanup() {
        // Clear any ongoing operations
        isProcessingRequest.set(false);
        
        // Clear references
        user = null;
        
        log.debug("SearchItemController cleanup completed");
    }
}
