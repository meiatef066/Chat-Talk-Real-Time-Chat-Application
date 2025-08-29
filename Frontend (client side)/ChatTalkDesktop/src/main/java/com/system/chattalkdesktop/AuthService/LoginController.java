package com.system.chattalkdesktop.AuthService;

import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.service.NotificationManager;
import com.system.chattalkdesktop.utils.*;
import com.system.chattalkdesktop.Dto.AuthDto.*;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class LoginController {
    public TextField email;
    public PasswordField password;

    public void NavigateToSignup(ActionEvent actionEvent) {
        NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/auth/Signup.fxml", "Sign Up 🦜🎉");
    }

    public void LoginButton(ActionEvent actionEvent) {
        String emailText = this.email.getText();
        String passwordText = this.password.getText();
        if (!Validation.isValidEmail(emailText) || !Validation.isValidPassword(passwordText)) {
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Please enter a valid email and password"
            );
            return;
        }

        Task<AuthResponse> loginTask = ApiAuthService.loginApi(LoginRequest.builder().email(emailText)
                .password(passwordText)
                .build());
        loginTask.setOnSucceeded(event -> {
            AuthResponse authResponse = loginTask.getValue();
            if (authResponse == null || authResponse.getToken() == null) {
                NotificationServiceImpl.getInstance().showErrorNotification(
                        "Login Failed",
                        "Invalid email or password"
                );
                return;
            }
            SessionManager.getInstance().storeSession(authResponse); // Save user & token
            NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/MainChat/ChatApp.fxml", "Welcome to Chat 🐿🎶");
//            NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/MainChat/ChatApp.fxml", "Welcome to Chat 🐿🎶");
            NotificationServiceImpl.getInstance().showSuccessNotification(
                    "Success ✅",
                    "Welcome " + authResponse.getUserDTO().getFirstName() + " " + authResponse.getUserDTO().getLastName() +
                            " , you are successfully logged in ✌🎶"
            );
            // Test WebSocket connection
            NotificationManager notificationManager = NotificationManager.getInstance();
            try {
                // Always attempt to connect and subscribe to notifications
                notificationManager.subscribeToNotifications();
                notificationManager.testConnection(); // Test basic connectivity
                System.out.println("🔌 WebSocket connection initiated after login");
            } catch (Exception e) {
                System.err.println("⚠️ WebSocket connection failed: " + e.getMessage());
                NotificationServiceImpl.getInstance().showErrorNotification(
                        "WebSocket Error",
                        "Failed to connect to notifications. Please try again later."
                );
            }
        });
        loginTask.setOnFailed(event -> {
            String errorMessage = event.getSource().getException().getMessage();
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Login Failed",
                    errorMessage
            );
            System.out.println("Login error: " + errorMessage);
        });

        new Thread(loginTask).start();
    }

    public void forgotPassword(ActionEvent actionEvent) {
        // Step 1: Get user's email
        String emailText = getEmailForPasswordReset();
        if (emailText == null || emailText.trim().isEmpty()) {
            return; // User cancelled
        }

        if (!Validation.isValidEmail(emailText)) {
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Invalid Email",
                    "Please enter a valid email address"
            );
            return;
        }

        // Step 2: Send reset code to email
        Task<Void> forgetPasswordTask = ApiAuthService.forgetPassword(emailText.trim());
        forgetPasswordTask.setOnSucceeded(event -> {
            NotificationServiceImpl.getInstance().showSuccessNotification(
                    "Code Sent ✅",
                    "Password reset code has been sent to your email"
            );
            // Step 3: Handle password reset
            handlePasswordReset(emailText.trim(), actionEvent);
        });
        forgetPasswordTask.setOnFailed(event -> {
            String errorMessage = event.getSource().getException().getMessage();
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Failed to Send Code",
                    errorMessage
            );
        });
        new Thread(forgetPasswordTask).start();
    }

    private String getEmailForPasswordReset() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Enter your email address");
        dialog.setContentText("Email:");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void handlePasswordReset(String email, ActionEvent actionEvent) {
        // Get verification code
        String code = getVerificationCodeForReset();
        if (code == null || code.trim().isEmpty()) {
            return; // User cancelled
        }

        // Get new password
        String newPassword = getNewPassword();
        if (newPassword == null || newPassword.trim().isEmpty()) {

            return; // User cancelled
        }

        if (!Validation.isValidPassword(newPassword)) {
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Invalid Password",
                    "Please enter a valid password"
            );
            return;
        }

        // Call reset password API
        Task<Void> resetPasswordTask = ApiAuthService.resetPassword(ResetPasswordRequest.builder()
                .email(email.trim())
                .code(code.trim())
                .newPassword(newPassword.trim())
                .build()
        );

        resetPasswordTask.setOnSucceeded(event -> {
            NotificationServiceImpl.getInstance().showSuccessNotification(
                    "Password Reset ✅",
                    "Your password has been reset successfully. You can now login with your new password."
            );
        });
        resetPasswordTask.setOnFailed(event -> {
            String errorMessage = event.getSource().getException().getMessage();
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Reset Failed",
                    errorMessage
            );
            // Ask if user wants to try again
            handlePasswordResetRetry(email, actionEvent);
        });

        new Thread(resetPasswordTask).start();
    }

    private String getVerificationCodeForReset() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password Reset");
        dialog.setHeaderText("Enter Verification Code");
        dialog.setContentText("Please enter the 6-digit code sent to your email:");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String getNewPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Password Reset");
        dialog.setHeaderText("Enter New Password");

        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        passwordField.setPromptText("New Password");
        confirmPasswordField.setPromptText("Confirm Password");

        VBox content = new VBox(10);
        content.getChildren().addAll(passwordField, confirmPasswordField);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Disable OK until both passwords match and not empty
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        ChangeListener<String> passwordListener = ( obs, oldVal, newVal) -> {
            boolean bothFilled = !passwordField.getText().trim().isEmpty()
                    && !confirmPasswordField.getText().trim().isEmpty();
            boolean match = passwordField.getText().equals(confirmPasswordField.getText());
            okButton.setDisable(!(bothFilled && match));
        };

        passwordField.textProperty().addListener(passwordListener);
        confirmPasswordField.textProperty().addListener(passwordListener);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }


    private void handlePasswordResetRetry(String email, ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Failed");
        alert.setHeaderText("Would you like to try again?");
        alert.setContentText("The verification code or password was incorrect. Do you want to try again?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            handlePasswordReset(email, actionEvent);
        }
    }
}