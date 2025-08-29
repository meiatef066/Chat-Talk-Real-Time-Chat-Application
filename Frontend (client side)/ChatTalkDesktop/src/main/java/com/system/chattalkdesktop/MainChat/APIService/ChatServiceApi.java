package com.system.chattalkdesktop.MainChat.APIService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.CashSessionManager;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatServiceApi {
    private static final String BASE_URL = "http://localhost:8080/api/chats";
    private static final ObjectMapper mapper = JacksonConfig.getObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    private static String getToken() {
        return "Bearer " + SessionManager.getInstance().getToken();
    }

    public static Task<Long> getOrCreatePrivateChat(String email2) {
        return new Task<>() {
            @Override
            protected Long call() {
                try {
                    String token = getToken();
//                    System.out.println("=== Getting or creating private chat with email: " + email2 + " using token: " + token.substring(0, Math.min(50, token.length())) + "... ===");

                    String uri = BASE_URL + "/private?email2=" + email2;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Private chat API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Private chat API response body: " + response.body() + " ===");

                    if (response.statusCode() == 200) {
                        Long chatId = mapper.readValue(response.body(), Long.class);
                        CashSessionManager.getInstance().addChatIdCash(email2, chatId);
                        System.out.println("Chat ID: " + chatId);
                             return chatId;
                    } else {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                        System.out.println("Error response: " + apiResponse.getMessage());
                           return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error in getOrCreatePrivateChat: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed to get or create chat", e.getMessage());
                    return null;
                }
            }
        };
    }
}