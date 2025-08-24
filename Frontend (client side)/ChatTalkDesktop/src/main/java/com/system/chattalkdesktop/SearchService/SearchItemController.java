package com.system.chattalkdesktop.SearchService;

import com.system.chattalkdesktop.Dto.AuthDto.FriendRequestResponse;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
    public void setUserData(SearchUserResultDTO userDTO)
    {
        this.user = userDTO;
        userEmail.setText(userDTO.getEmail());
        userName.setText(userDTO.getFirstName() + " " + userDTO.getLastName());
        if (userDTO.getProfilePictureUrl()!=null)
        {
            profileImage.setImage( new Image(userDTO.getProfilePictureUrl()));
        }
        // Set button state based on relationshipStatus
        String status = userDTO.getRelationshipStatus();
        if ("PENDING".equals(status)) {
            addButton.setDisable(true);
            addButton.setText("Pending Request");
            addButton.setStyle("-fx-background-color: #595959; -fx-text-fill: white;");
        } else if ("ACCEPTED".equals(status)) {
            addButton.setDisable(true);
            addButton.setText("Accepted");
            addButton.setStyle("-fx-background-color: #64fa7d; -fx-text-fill: white;");
        } else if ("BLOCKED".equals(status)) {
            addButton.setDisable(true);
            addButton.setText("Blocked");
            addButton.setStyle("-fx-background-color: #f45050; -fx-text-fill: white;");
        } else {
            addButton.setDisable(false);
            addButton.setText("Add Contact");
            addButton.setStyle("");
        }
    }
    @FXML
    public void addUser( ActionEvent actionEvent ) {
        addButton.setDisable(true);
        Task<FriendRequestResponse> friendRequestResponseTask = ApiSearchUsers.sendFriendRequest(user.getEmail());
        friendRequestResponseTask.setOnSucceeded(e -> {
            addButton.setDisable( true );
            addButton.setText("Pending Request");
            addButton.setStyle("-fx-background-color: #595959; -fx-text-fill: white;");
            NotificationServiceImpl.getInstance().showSuccessNotification("Successfully","request sent successfully 💌🎶");
        });
        friendRequestResponseTask.setOnFailed(e -> {
            addButton.setDisable( false );
            NotificationServiceImpl.getInstance().showErrorNotification("failed","error occurred");
        });
        new Thread(friendRequestResponseTask).start();
    }
}
