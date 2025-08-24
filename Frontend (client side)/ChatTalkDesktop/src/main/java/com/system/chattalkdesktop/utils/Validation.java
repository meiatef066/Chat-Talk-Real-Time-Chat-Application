package com.system.chattalkdesktop.utils;


import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;

public class Validation {

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Please write a valid email address");
            return false;
        }
        return true;
    }

    public static boolean isValidPassword(String password) {
        if (password != null && password.length() >= 6) {
            return true;
        }
        NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Password should be more than 6 characters");
        return false;
    }

    public static boolean isValidPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty() || !phoneNumber.matches("^\\+?\\d{10,15}$")) {
            NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Please write a valid phone number");
            return false;
        }
        return true;
    }

    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Name cannot be empty");
            return false;
        }
        return true;
    }

    public static boolean isValidAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Address cannot be empty");
            return false;
        }
        return true;
    }
    
    // Simple URL validation (can be enhanced as needed)
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty() || !url.matches("^(https?|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")) {
            NotificationServiceImpl.getInstance().showErrorNotification("Invalid Data", "Please write a valid URL");
            return false;
        }
        return true;
    }
}
