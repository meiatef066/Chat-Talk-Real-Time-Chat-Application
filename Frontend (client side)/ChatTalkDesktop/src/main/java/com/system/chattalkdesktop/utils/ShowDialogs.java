package com.system.chattalkdesktop.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class ShowDialogs {

    private static final String STYLE_PATH = "/CSS/style.css"; // adjust path if needed

    public static void showWarningDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText("Action Required!");
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }
    public static void showErrorDialog(String title,String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong!");
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }
    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong!");
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    public static void showInfoDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("Success!");
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private static void styleDialog(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ShowDialogs.class.getResource(STYLE_PATH).toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
    }
}
