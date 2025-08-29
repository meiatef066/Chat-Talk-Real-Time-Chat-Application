package com.system.chattalkdesktop;

import com.system.chattalkdesktop.service.ChatManagerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize chat manager service for real-time functionality
        initializeChatServices();
        
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Auth/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("ChatTalk Desktop - Real-time Chat Application");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Initialize chat services for real-time functionality
     */
    private void initializeChatServices() {
        try {
            // Initialize the chat manager service
            ChatManagerService chatManager = ChatManagerService.getInstance();
            chatManager.initialize();
            
            System.out.println("Chat services initialized successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize chat services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        // Shutdown chat services gracefully
        try {
            ChatManagerService chatManager = ChatManagerService.getInstance();
            chatManager.shutdown();
            System.out.println("Chat services shutdown successfully");
        } catch (RuntimeException e) {
            System.err.println("Error during chat services shutdown: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error during chat services shutdown: " + e.getMessage());
            e.printStackTrace();
        }
        
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}