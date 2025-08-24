package com.system.chattalkdesktop.Profile;

import com.system.chattalkdesktop.Dto.UpdateProfileRequest;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.ShowDialogs;
import com.system.chattalkdesktop.utils.Validation;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ProfileController {
    @FXML
    public ImageView profileImage;
    @FXML
    public TextField firstName;
    @FXML
    public TextField lastName;
    @FXML
    public TextField email;
    @FXML
    public Label errorLabel;
    @FXML
    public TextField phoneNumber;
    @FXML
    public TextArea bio;
    @FXML
    public ComboBox<String> gender;
    @FXML
    public DatePicker dateOfBirth;
    @FXML
    public Button saveButton;
    @FXML
    private ProgressIndicator loadingIndicator;

    private static final String[] ALLOWED_MIME_TYPES = {"image/png", "image/jpeg", "image/jpg"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private UserDTO user;
    private File selectedProfilePicture;

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(true);
        Task<UserDTO> loadProfileTask = ApiProfileService.getProfile();
        loadProfileTask.setOnSucceeded(e -> {
            user = loadProfileTask.getValue();
            updateUI();
            loadingIndicator.setVisible(false);
        });
        loadProfileTask.setOnFailed(e -> {
            NotificationServiceImpl.getInstance().showErrorNotification("Failed", "Error loading profile");
            loadingIndicator.setVisible(false);
        });
        new Thread(loadProfileTask).start();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            firstName.setText(user.getFirstName() != null ? user.getFirstName() : "");
            lastName.setText(user.getLastName() != null ? user.getLastName() : "");
            email.setText(user.getEmail() != null ? user.getEmail() : "");
            phoneNumber.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            bio.setText(user.getBio() != null ? user.getBio() : "");
            gender.setValue(user.getGender() != null ? user.getGender() : "Prefer not to say");

            // Set date of birth
            if (user.getDateOfBirth() != null) {
                LocalDate localDate = user.getDateOfBirth().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                System.out.println(localDate);
                dateOfBirth.setValue(localDate);
            }

            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                try {
                    profileImage.setImage(new Image(user.getProfilePictureUrl(), true));
                } catch (Exception ex) {
                    System.out.println("Failed to load profile picture: " + ex.getMessage());
                    profileImage.setImage(null);
                }
            }
            validateAllFields();
        });
    }

    @FXML
    public void changeProfilePicture( ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        selectedProfilePicture = fileChooser.showOpenDialog(null);

        if (selectedProfilePicture != null) {
            try {
                String mimeType = Files.probeContentType(selectedProfilePicture.toPath());
                if (mimeType == null || !isAllowedMimeType(mimeType)) {
                    ShowDialogs.showWarningDialog("Invalid image file type. Please select a PNG, JPG, or JPEG file.");
                    selectedProfilePicture = null;
                    return;
                }
                if (selectedProfilePicture.length() > MAX_FILE_SIZE) {
                    ShowDialogs.showWarningDialog("Image file size exceeds 5MB limit.");
                    selectedProfilePicture = null;
                    return;
                }
                profileImage.setImage(new Image(selectedProfilePicture.toURI().toString()));
            } catch (IOException e) {
                ShowDialogs.showErrorDialog("Error validating image file: " + e.getMessage());
                selectedProfilePicture = null;
            }
            validateAllFields();
        }
    }

    @FXML
    public void saveProfile(ActionEvent actionEvent) {
        validateAllFields();
        if (saveButton.isDisabled()) {
            ShowDialogs.showWarningDialog("Please correct all fields before saving.");
            return;
        }

        String formattedDateOfBirth = null;
        if (dateOfBirth.getValue() != null) {
            LocalDate selectedDate = dateOfBirth.getValue();
            LocalDate today = LocalDate.now();

            if (selectedDate.isAfter(today)) {
                ShowDialogs.showWarningDialog("Date of birth cannot be in the future.");
                return;
            }

            formattedDateOfBirth = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            System.out.println("Formatted date: " + formattedDateOfBirth);
        } else {
            System.out.println("No date selected");
        }
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName(firstName.getText().trim())
                .lastName(lastName.getText().trim())
                .gender(gender.getValue())
                .bio(bio.getText().trim())
                .phoneNumber(phoneNumber.getText().trim())
                .dateOfBirth(formattedDateOfBirth)
                .build();

        // Debug logging
        System.out.println("Built request:");
        System.out.println("  firstName: " + request.getFirstName());
        System.out.println("  lastName: " + request.getLastName());
        System.out.println("  gender: " + request.getGender());
        System.out.println("  bio: " + request.getBio());
        System.out.println("  phoneNumber: " + request.getPhoneNumber());
        System.out.println("  dateOfBirth: " + request.getDateOfBirth());

        // First update profile data
        Task<UserDTO> saveProfileTask = ApiProfileService.updateProfile(request);
        saveProfileTask.setOnSucceeded(e -> {
            UserDTO updatedUser = saveProfileTask.getValue();
            if (updatedUser != null) {
                user = updatedUser;

                // If there's a profile picture, update it separately
                if (selectedProfilePicture != null) {
                    Task<UserDTO> updatePictureTask = ApiProfileService.updateProfilePicture(selectedProfilePicture);
                    updatePictureTask.setOnSucceeded(pictureEvent -> {
                        UserDTO pictureUpdatedUser = updatePictureTask.getValue();
                        if (pictureUpdatedUser != null) {
                            user = pictureUpdatedUser;
                            selectedProfilePicture = null;
                            NotificationServiceImpl.getInstance().showSuccessNotification("Success", "Profile and picture updated successfully!");
                        }
                    });
                    updatePictureTask.setOnFailed(pictureEvent -> {
                        NotificationServiceImpl.getInstance().showErrorNotification("Warning", "Profile updated but picture update failed");
                    });
                    new Thread(updatePictureTask).start();
                } else {
                    selectedProfilePicture = null;
                    NotificationServiceImpl.getInstance().showSuccessNotification("Success", "Profile updated successfully!");
                }
            }
        });
        saveProfileTask.setOnFailed(e -> {
            NotificationServiceImpl.getInstance().showErrorNotification("Error", "Failed to save profile");
        });
        new Thread(saveProfileTask).start();
    }
    private void validateAllFields() {
        StringBuilder errorMessage = new StringBuilder();
        boolean isValid = true;

        if (firstName.getText().trim().isEmpty()) {
            errorMessage.append("First name is required.\n");
            isValid = false;
        }
        if (lastName.getText().trim().isEmpty()) {
            errorMessage.append("Last name is required.\n");
            isValid = false;
        }
        if (!phoneNumber.getText().trim().isEmpty() && !Validation.isValidPhone(phoneNumber.getText().trim())) {
            errorMessage.append("Invalid phone number format.\n");
            isValid = false;
        }

        errorLabel.setText(errorMessage.toString());
        saveButton.setDisable(!isValid);
    }

    private boolean isAllowedMimeType(String mimeType) {
        for (String allowedType : ALLOWED_MIME_TYPES) {
            if (allowedType.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    public void cancelChanges( ActionEvent actionEvent ) {
    }
}
