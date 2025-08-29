package com.system.chattalkdesktop.MainChat.APIService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.Dto.ChatDto.SendMessageRequest;
import com.system.chattalkdesktop.Dto.entity.MessageDTO;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiChatService {
    private static final String BASE_URL = "http://localhost:8080/api/chats";
    private static final ObjectMapper mapper = JacksonConfig.getObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    private static String getToken() {
        return "Bearer " + SessionManager.getInstance().getToken();
    }

    public static Task<List<MessageDTO>> getChatHistory(Long chatId, int page, int size) {
        return new Task<>() {
            @Override
            protected List<MessageDTO> call() {
                try {
                    String token = getToken();
                    System.out.println("=== Getting chat history for chat ID: " + chatId + " (page: " + page + ", size: " + size + ") ===");
                    
                    String uri = BASE_URL + "/" + chatId + "/messages?page=" + page + "&size=" + size;
                    System.out.println("=== Request URI: " + uri + " ===");
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Chat history API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Response body: " + response.body() + " ===");

                    if (response.statusCode() == 200) {
                        try {
                            // Backend returns List<MessageDTO> directly, not wrapped in ApiResponse
                            List<MessageDTO> messages = mapper.readValue(response.body(), new TypeReference<List<MessageDTO>>() {});
                            System.out.println("=== Successfully parsed " + (messages != null ? messages.size() : 0) + " messages ===");
                            return messages;
                        } catch (Exception parseException) {
                            System.err.println("=== Error parsing response as List<MessageDTO>: " + parseException.getMessage() + " ===");
                            System.err.println("=== Raw response: " + response.body() + " ===");
                            throw parseException;
                        }
                    }
                    
                    System.err.println("=== API request failed with status: " + response.statusCode() + " ===");
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed to get chat history", "Status: " + response.statusCode());
                    return null;
                } catch (Exception e) {
                    System.err.println("Error in getChatHistory: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to get chat history: " + e.getMessage());
                    return null;
                }
            }
        };
    }

    public static Task<MessageDTO> sendMessage(Long chatId, SendMessageRequest messageRequest) {
        return new Task<>() {
            @Override
            protected MessageDTO call() {
                try {
                    String token = getToken();
                    System.out.println("=== Sending message to chat ID: " + chatId + " ===");
                    
                    messageRequest.setChatId(chatId);
                    String messageJson = mapper.writeValueAsString(messageRequest);
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/" + chatId + "/messages"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .POST(HttpRequest.BodyPublishers.ofString(messageJson))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Send message API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Response body: " + response.body() + " ===");

                    if (response.statusCode() == 201) {
                        // Backend returns MessageDTO directly, not wrapped in ApiResponse
                        return mapper.readValue(response.body(), MessageDTO.class);
                    }
                    
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed to send message", "Status: " + response.statusCode());
                    return null;
                } catch (Exception e) {
                    System.err.println("Error in sendMessageButton: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to send message: " + e.getMessage());
                    return null;
                }
            }
        };
    }

    public static Task<Integer> getUnreadCount(Long chatId, Long currentUserId) {
        return new Task<>() {
            @Override
            protected Integer call() {
                try {
                    String token = getToken();
                    System.out.println("=== Getting unread count for chat ID: " + chatId + " ===");
                    
                    // Backend endpoint is /{chatId}/unread (no currentUserId parameter needed)
                    String uri = BASE_URL + "/" + chatId + "/unread";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Unread count API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Response body: " + response.body() + " ===");

                    if (response.statusCode() == 200) {
                        // Backend returns Long directly, not wrapped in ApiResponse
                        Long unreadCount = mapper.readValue(response.body(), Long.class);
                        return unreadCount.intValue();
                    }
                    
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed to get unread count", "Status: " + response.statusCode());
                    return null;
                } catch (Exception e) {
                    System.err.println("Error in getUnreadCount: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to get unread count: " + e.getMessage());
                    return null;
                }
            }
        };
    }

    public static Task<Void> markMessagesAsRead(Long chatId, Long currentUserId) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    String token = getToken();
                    System.out.println("=== Marking messages as read for chat ID: " + chatId + " ===");
                    
                    // Backend endpoint is PATCH /{chatId}/read (no currentUserId parameter needed)
                    String uri = BASE_URL + "/" + chatId + "/read";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .method("PATCH", HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Mark as read API response status: " + response.statusCode() + " ===");

                    if (response.statusCode() != 204) { // 204 No Content is expected
                        System.err.println("=== Unexpected status code for mark as read: " + response.statusCode() + " ===");
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Failed to mark messages as read", "Status: " + response.statusCode());
                    } else {
                        System.out.println("=== Messages marked as read successfully ===");
                    }
                    
                    return null;
                } catch (Exception e) {
                    System.err.println("Error in markMessagesAsRead: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to mark messages as read: " + e.getMessage());
                    return null;
                }
            }
        };
    }
}
