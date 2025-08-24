package com.system.chattalkdesktop.AuthService;

import com.system.chattalkdesktop.Dto.AuthDto.AuthResponse;
import com.system.chattalkdesktop.Dto.AuthDto.RegisterRequest;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.NavigationUtil;
import com.system.chattalkdesktop.utils.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.util.Optional;

public class SignUpController {
    public TextField firstName;
    public TextField lastName;
    public TextField email;
    public PasswordField password;
    public PasswordField confirmPassword;
    public CheckBox agreeTermsAndConditions;

    public void NavigateToLogin( ActionEvent actionEvent) {
        NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/auth/Login.fxml", "Login 🦜🎉");
    }

    public void SignUpButton(ActionEvent actionEvent) {
        System.out.println("SignUpButton clicked!"); // Debug log

        String firstNameText = this.firstName.getText();
        String lastNameText = this.lastName.getText();
        String emailText = this.email.getText();
        String passwordText = this.password.getText();
        String confirmPasswordText = this.confirmPassword.getText();

        System.out.println("Form data - FirstName: " + firstNameText + ", LastName: " + lastNameText + ", Email: " + emailText); // Debug log

        // Validate all fields
        if (!Validation.isValidName(firstNameText)) {
            System.out.println("Validation failed: Invalid first name");
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Please enter a valid first name"
            );
            return;
        }
        if (!Validation.isValidName(lastNameText)) {
            System.out.println("Validation failed: Invalid last name");
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Please enter a valid last name"
            );
            return;
        }
        if (!Validation.isValidEmail(emailText)) {
            System.out.println("Validation failed: Invalid email");
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Please enter a valid email"
            );
            return;
        }
        if (!Validation.isValidPassword(passwordText)) {
            System.out.println("Validation failed: Invalid password");
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Please enter a valid password"
            );
            return;
        }
        System.out.println("All validations passed!");

        // Check if terms and conditions are agreed to
        if (!agreeTermsAndConditions.isSelected()) {
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Terms Required",
                    "Please agree to the terms and conditions to continue"
            );
            return;
        }

        if (!passwordText.equals(confirmPasswordText)) {
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Validation Error",
                    "Passwords do not match"
            );
            return;
        }

        Task<AuthResponse> registerTask1 = ApiAuthService.signUpApi(
                RegisterRequest.builder()
                        .email(emailText)
                        .firstName(firstNameText)
                        .lastName(lastNameText)
                        .password(passwordText)
                        .build()
        );

        registerTask1.setOnSucceeded(event -> {
            System.out.println("Registration SUCCESS callback received!");
            AuthResponse authResponse = registerTask1.getValue();

            if (authResponse != null && authResponse.getUserDTO() != null) {
                SessionManager.getInstance().storeSession(authResponse);
                NotificationServiceImpl.getInstance().showSuccessNotification(
                        "Success ✅",
                        "Welcome " + authResponse.getUserDTO().getFirstName() + " " + authResponse.getUserDTO().getLastName() +
                                " , you are successfully registered! A verification code has been sent to your email."
                );
                handleEmailVerification(emailText, actionEvent);
            }

        });

        // Error handler (UI thread)
        registerTask1.setOnFailed(event -> {
            Throwable ex = registerTask1.getException();
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Registration Failed",
                    ex != null ? ex.getMessage() : "Unknown error"
            );
        });
        // Run task in background
        Thread thread = new Thread(registerTask1);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleEmailVerification(String email, ActionEvent actionEvent) {
        String verificationCode = showVerifyDialog();

        if (verificationCode != null && !verificationCode.trim().isEmpty()) {
            // Validate the verification code format (assuming 6 digits)
            if (!isValidVerificationCode(verificationCode.trim())) {
                NotificationServiceImpl.getInstance().showErrorNotification(
                        "Invalid Code",
                        "Please enter a valid 6-digit verification code"
                );
                // Show dialog again
                handleEmailVerification(email, actionEvent);
                return;
            }

            // Call the verification API
            Task<Void> verifyTask = ApiAuthService.verifyEmail(email.trim(), verificationCode.trim());

            verifyTask.setOnSucceeded(event -> {
                NotificationServiceImpl.getInstance().showSuccessNotification(
                        "Email Verified ✅",
                        "Your email has been verified successfully! You can now login."
                );
                NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/auth/Login.fxml", "Login 🦜🎉");
            });

            verifyTask.setOnFailed(event -> {
                String errorMessage = event.getSource().getException().getMessage();
                NotificationServiceImpl.getInstance().showErrorNotification(
                        "Verification Failed",
                        errorMessage
                );
                // Ask user if they want to try again
                handleVerificationRetry(email, actionEvent);
            });
            new Thread(verifyTask).start();
        } else {
            // User cancelled or entered empty code
            NotificationServiceImpl.getInstance().showErrorNotification(
                    "Verification Required",
                    "Please verify your email to complete registration"
            );

            // Navigate to login anyway, but user won't be able to login until verified
            NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/auth/Login.fxml", "Login 🦜🎉");
        }
    }

    private void handleVerificationRetry(String email, ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Verification Failed");
        alert.setHeaderText("Would you like to try again?");
        alert.setContentText("The verification code was incorrect. Do you want to enter it again?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            handleEmailVerification(email, actionEvent);
        } else {
            NavigationUtil.switchScene(actionEvent, "/com/system/chattalkdesktop/auth/Login.fxml", "Login 🦜🎉");
        }
    }

    private boolean isValidVerificationCode(String code) {
        // Check if code is 6 digits
        return code.matches("\\d{6}");
    }

    public String showVerifyDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Verification");
        dialog.setHeaderText("Enter Verification Code");
        dialog.setContentText("Please enter the 6-digit code sent to your email:");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> System.out.println("User name: " + name));
        return result.orElse(null); // return null if canceled
    }
}