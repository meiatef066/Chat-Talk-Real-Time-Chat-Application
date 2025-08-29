package com.system.chattalkdesktop.MainChat;

import com.system.chattalkdesktop.Dto.PendingFriendRequestDto;
import com.system.chattalkdesktop.MainChat.APIService.ApiContactService;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PendingItemController {
    @FXML
    public HBox requestItem;
    @FXML
    private ImageView avatar;
    @FXML
    private Label userName;
    @FXML
    private Label userEmail;
    @FXML
    private Button acceptButton;
    @FXML
    private Button rejectButton;
    private PendingFriendRequestDto request;

    private Runnable onRequestAction; // to refresh contact list


    public void setRequestData( PendingFriendRequestDto request ) {
        this.request = request;
        userName.setText((request.getFirstName() + " " + request.getLastName()).trim());
        userEmail.setText(request.getEmail() != null && !request.getEmail().isEmpty() ? request.getEmail() : "No email");
        if (request.getProfilePictureUrl() != null && !request.getProfilePictureUrl().isEmpty()) {
            avatar.setImage(new Image(request.getProfilePictureUrl(), true));
        }
    }

    @FXML
    public void onAcceptButtonClick() {
        if (request != null) {
            Task<Void> acceptRequestTask= ApiContactService.acceptRequest(request.getRequestId());
            acceptRequestTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    // Refresh the lists
                    if (onRequestAction != null) {
                        onRequestAction.run();
                    }
                });
            });
            acceptRequestTask.setOnFailed(e->{
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification("Error", "Failed to accept request"));
            });

            new Thread(acceptRequestTask).start();
        }
    }

    @FXML
    public void onRejectButtonClick() {
        if (request != null) {
            Task<Void> rejectRequestTask = ApiContactService.rejectRequest(request.getRequestId());
            rejectRequestTask.setOnSucceeded(event -> {
                Platform.runLater(() -> {
                    // Refresh the lists
                    if (onRequestAction != null) {
                        onRequestAction.run();
                    }
                });
            });
            rejectRequestTask.setOnFailed(event -> {
                Platform.runLater(() -> NotificationServiceImpl.getInstance().showErrorNotification("Error", "Failed to reject request"));
            });
            new Thread(rejectRequestTask).start();
        }
    }

}
