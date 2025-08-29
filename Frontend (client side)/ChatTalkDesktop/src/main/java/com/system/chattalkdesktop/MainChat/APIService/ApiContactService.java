package com.system.chattalkdesktop.MainChat.APIService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.Dto.PendingFriendRequestDto;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiContactService {
    private static final String BASE_URL = "http://localhost:8080/api/contacts";
    static ObjectMapper mapper = JacksonConfig.getObjectMapper();
    static HttpClient client = HttpClient.newHttpClient();

    // Remove static TOKEN and get it dynamically
    private static String getToken() {
        return "Bearer " + SessionManager.getInstance().getToken();
    }

    public static Task<List<PendingFriendRequestDto>> getPendingRequests() {
        return new Task<>() {
            @Override
            protected List<PendingFriendRequestDto> call() {
                try {
                    String token = getToken();
                    System.out.println("=== Getting pending requests with token: " + token.substring(0, Math.min(50, token.length())) + "... ===");
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/requests/pending"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Pending requests API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Pending requests API response body: " + response.body() + " ===");

                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("pendingRequests")) {
                        System.out.println("Pending requests: " + apiResponse.getData().get("pendingRequests"));
                        return mapper.convertValue(
                                apiResponse.getData().get("pendingRequests"),
                                new TypeReference<List<PendingFriendRequestDto>>() {}
                        );
                    } else {
                        System.out.println("No pendingRequests key in response data: " + apiResponse.getData());
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Fetch pending request failed", apiResponse.getMessage());
                        return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error in getPendingRequests: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Task<List<UserDTO>> getFriendList() {
        return new Task<>() {
            @Override
            protected List<UserDTO> call() {
                try {
                    String token = getToken();
                    System.out.println("=== Getting friend list with token: " + token.substring(0, Math.min(50, token.length())) + "... ===");
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/friends"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("=== Friend list API response status: " + response.statusCode() + " ===");
                    System.out.println("=== Friend list API response body: " + response.body() + " ===");
                    
                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("friendList")) {
                        System.out.println("Friends: " + apiResponse.getData().get("friendList"));
                        return mapper.convertValue(
                                apiResponse.getData().get("friendList"),
                                new TypeReference<List<UserDTO>>() {}
                        );
                    } else {
                        System.out.println("No friendList key in response data: " + apiResponse.getData());
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Fetch friend list failed", apiResponse.getMessage());
                        return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error in getFriendList: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Task<Void> rejectRequest(Long requestId) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/requests/"+requestId+"/reject"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", getToken())
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (response.statusCode() == 200) {
                        NotificationServiceImpl.getInstance()
                                .showSuccessNotification("Request Rejected successfully", apiResponse.getMessage());
                    } else {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Reject Failed", apiResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Reject Failed", e.getMessage());
                }
                return null;
            }
        };
    }

    public static Task<Void> acceptRequest(Long requestId) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/requests/"+requestId+"/accept"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", getToken())
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (response.statusCode() == 200) {
                        NotificationServiceImpl.getInstance()
                                .showSuccessNotification("Request Accepted successfully🛺", apiResponse.getMessage());
                    } else {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Failed", apiResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed", e.getMessage());
                }
                return null;
            }
        };
    }


}