package com.system.chattalkdesktop.SearchService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.Dto.AuthDto.FriendRequestResponse;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.service.PerformanceOptimizationService;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ApiSearchUsers {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final ObjectMapper mapper = JacksonConfig.getObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static class PagedSearchResults {
        private final List<SearchUserResultDTO> results;
        private final int page;
        private final int totalPages;

        public PagedSearchResults(List<SearchUserResultDTO> results, int page, int totalPages) {
            this.results = results;
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<SearchUserResultDTO> getResults() {
            return results;
        }

        public int getPage() {
            return page;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }

    // Retrieve token dynamically
    private static String getToken() {
        String token = SessionManager.getInstance().getToken();
        return token != null ? "Bearer " + token : "";
    }

    /**
     * Send a friend request to a given email
     */
    public static Task<FriendRequestResponse> sendFriendRequest( String email ) {
        return new Task<>() {
            @Override
            protected FriendRequestResponse call() {
                try {
                    String token = getToken();
                    if (token.isEmpty()) {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Authentication Error", "No token found. Please log in again.");
                        return null;
                    }

                    // Build HTTP request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/contacts/requests?receiverEmail=" + email))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    // Send request
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // Handle response
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                        if (apiResponse.getData() != null && apiResponse.getData().containsKey("response")) {
                            Object rawResponse = apiResponse.getData().get("response");
                            return mapper.convertValue(rawResponse, FriendRequestResponse.class);
                        } else {
                            NotificationServiceImpl.getInstance()
                                    .showErrorNotification("Friend Request Failed", apiResponse.getMessage());
                        }
                    } else {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Friend Request Failed", "HTTP " + response.statusCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    /**
     * Search users by query (name/email) excluding current user
     */
    public static Task<PagedSearchResults> searchUsers( String query, Long currentUserId, int page, int size ) {
        return new Task<>() {
            @Override
            protected PagedSearchResults call() {
                try {
                    String token = getToken();
                    if (token.isEmpty()) {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Authentication Error", "No token found. Please log in again.");
                        return null;
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }

                    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                    String url = String.format("%s/users/search?query=%s&currentUserId=%d&page=%d&size=%d",
                            BASE_URL, encodedQuery, currentUserId, page, size);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Search API Response Status: " + response.statusCode());
                    System.out.println("Search API Response Body: " + response.body());

                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                        if (apiResponse.getData() != null && apiResponse.getData().containsKey("results")) {
                            Object rawList = apiResponse.getData().get("results");
                            List<SearchUserResultDTO> users = mapper.convertValue(rawList, new TypeReference<List<SearchUserResultDTO>>() {});

                            int currentPage = mapper.convertValue(apiResponse.getData().get("page"), Integer.class);
                            int totalPages = mapper.convertValue(apiResponse.getData().get("totalPages"), Integer.class);

                            return new PagedSearchResults(users, currentPage, totalPages);
                        } else {
                            NotificationServiceImpl.getInstance()
                                    .showErrorNotification("Search Failed", apiResponse.getMessage());
                        }
                    } else {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Search Failed", "HTTP " + response.statusCode());
                    }


                } catch (Exception e) {
                    if (e instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                        return null; // Swallow interrupts from cancelled search
                    }
                    // Log minimal to avoid console spam
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to load search results.");
                }
                return null;
            }
        };
    }
}

