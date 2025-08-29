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

/**
 * Optimized API service for user search functionality
 * Implements caching, async operations, and proper thread safety
 */
@Slf4j
public class ApiSearchUsers {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final ObjectMapper mapper = JacksonConfig.getObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();
    
    // Thread-safe singleton for performance optimization
    private static final PerformanceOptimizationService performanceService = PerformanceOptimizationService.getInstance();
    
    // Cache for search results (query + userId -> results)
    private static final Map<String, CachedSearchResult> searchCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(5); // 5 minutes
    
    // Cache for current search to prevent duplicate requests
    private static final AtomicReference<String> lastSearchQuery = new AtomicReference<>();
    private static final AtomicReference<Long> lastSearchUserId = new AtomicReference<>();
    
    // Cache key generator
    private static String generateCacheKey(String query, Long userId, int page, int size) {
        return String.format("%s_%d_%d_%d", query.toLowerCase().trim(), userId, page, size);
    }
    
    // Cached search result wrapper
    private static class CachedSearchResult {
        private final PagedSearchResults results;
        private final long timestamp;
        
        public CachedSearchResult(PagedSearchResults results) {
            this.results = results;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
        
        public PagedSearchResults getResults() {
            return results;
        }
    }

    public static class PagedSearchResults {
        private final List<SearchUserResultDTO> results;
        private final int page;
        private final int totalPages;
        private final int totalElements;
        private final int size;

        public PagedSearchResults(List<SearchUserResultDTO> results, int page, int totalPages, int totalElements, int size) {
            this.results = results;
            this.page = page;
            this.totalPages = totalPages;
            this.totalElements = totalElements;
            this.size = size;
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

        public int getTotalElements() {
            return totalElements;
        }

        public int getSize() {
            return size;
        }
    }

    /**
     * Get authentication token with validation
     */
    private static String getToken() {
        String token = SessionManager.getInstance().getToken();
        if (token == null || token.trim().isEmpty()) {
            log.warn("No authentication token available");
            return null;
        }
        return "Bearer " + token;
    }

    /**
     * Validate current user session
     */
    private static Long getCurrentUserId() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            log.warn("No current user available");
            return null;
        }
        return currentUser.getId();
    }

    /**
     * Send a friend request to a given email with improved error handling
     */
    public static Task<FriendRequestResponse> sendFriendRequest(String email) {
        return new Task<>() {
            @Override
            protected FriendRequestResponse call() throws Exception {
                if (email == null || email.trim().isEmpty()) {
                    throw new IllegalArgumentException("Email cannot be null or empty");
                }

                String token = getToken();
                if (token == null) {
                    throw new IllegalStateException("No authentication token available");
                }

                // Build HTTP request
                String encodedEmail = URLEncoder.encode(email.trim(), StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/contacts/requests?receiverEmail=" + encodedEmail))
                        .header("Content-Type", "application/json")
                        .header("Authorization", token)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofSeconds(10))
                        .build();

                log.debug("Sending friend request to: {}", email);

                // Send request with timeout
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Handle response
                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("response")) {
                        Object rawResponse = apiResponse.getData().get("response");
                        FriendRequestResponse friendResponse = mapper.convertValue(rawResponse, FriendRequestResponse.class);
                        log.debug("Friend request sent successfully to: {}", email);
                        return friendResponse;
                    } else {
                        String errorMessage = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Unknown error";
                        log.error("Friend request failed: {}", errorMessage);
                        throw new RuntimeException("Friend request failed: " + errorMessage);
                    }
                } else {
                    String errorMessage = "HTTP " + response.statusCode() + ": " + response.body();
                    log.error("Friend request failed: {}", errorMessage);
                    throw new RuntimeException(errorMessage);
                }
            }
        };
    }

    /**
     * Search users by query with caching and improved performance
     */
    public static Task<PagedSearchResults> searchUsers(String query, Long currentUserId, int page, int size) {
        return new Task<>() {
            @Override
            protected PagedSearchResults call() throws Exception {
                // Input validation
                if (query == null || query.trim().isEmpty()) {
                    throw new IllegalArgumentException("Search query cannot be null or empty");
                }
                
                if (currentUserId == null) {
                    throw new IllegalArgumentException("Current user ID cannot be null");
                }

                if (page < 0) {
                    throw new IllegalArgumentException("Page number cannot be negative");
                }

                if (size <= 0 || size > 100) {
                    throw new IllegalArgumentException("Page size must be between 1 and 100");
                }

                // Check for cancellation
                if (isCancelled()) {
                    log.debug("Search task cancelled for query: {}", query);
                    return null;
                }

                // Check cache first
                String cacheKey = generateCacheKey(query, currentUserId, page, size);
                CachedSearchResult cachedResult = searchCache.get(cacheKey);
                if (cachedResult != null && cachedResult.isValid()) {
                    log.debug("Returning cached search results for query: {}", query);
                    return cachedResult.getResults();
                }

                String token = getToken();
                if (token == null) {
                    throw new IllegalStateException("No authentication token available");
                }

                // Build URL with proper encoding
                String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
                String url = String.format("%s/users/search?query=%s&currentUserId=%d&page=%d&size=%d",
                        BASE_URL, encodedQuery, currentUserId, page, size);

                log.debug("Searching users with query: '{}', page: {}, size: {}", query, page, size);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", token)
                        .header("Accept", "application/json")
                        .timeout(java.time.Duration.ofSeconds(15))
                        .GET()
                        .build();

                // Send request asynchronously
                CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                
                // Wait for response with cancellation check
                HttpResponse<String> response = null;
                try {
                    response = futureResponse.get(15, TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.error("Search request timed out for query: {}", query);
                    throw new RuntimeException("Search request timed out");
                }

                // Check for cancellation before processing response
                if (isCancelled()) {
                    log.debug("Search task cancelled after response for query: {}", query);
                    return null;
                }

                if (response.statusCode() == 200) {
                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("results")) {
                        Object rawList = apiResponse.getData().get("results");
                        List<SearchUserResultDTO> users = mapper.convertValue(rawList, 
                                new TypeReference<List<SearchUserResultDTO>>() {});

                        int currentPage = mapper.convertValue(apiResponse.getData().get("page"), Integer.class);
                        int totalPages = mapper.convertValue(apiResponse.getData().get("totalPages"), Integer.class);
                        int totalElements = mapper.convertValue(apiResponse.getData().get("totalElements"), Integer.class);
                        int pageSize = mapper.convertValue(apiResponse.getData().get("size"), Integer.class);

                        PagedSearchResults results = new PagedSearchResults(users, currentPage, totalPages, totalElements, pageSize);
                        
                        // Cache the results
                        searchCache.put(cacheKey, new CachedSearchResult(results));
                        
                        log.debug("Search completed: found {} users, page {}/{}", users.size(), currentPage + 1, totalPages);

                        return results;
                    } else {
                        String errorMessage = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Invalid response format";
                        log.error("Search failed: {}", errorMessage);
                        throw new RuntimeException("Search failed: " + errorMessage);
                    }
                } else {
                    String errorMessage = "HTTP " + response.statusCode() + ": " + response.body();
                    log.error("Search failed: {}", errorMessage);
                    throw new RuntimeException(errorMessage);
                }
            }
        };
    }

    /**
     * Search users with automatic current user ID retrieval
     */
    public static Task<PagedSearchResults> searchUsers(String query, int page, int size) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Task<PagedSearchResults> failedTask = new Task<>() {
                @Override
                protected PagedSearchResults call() throws Exception {
                    throw new IllegalStateException("No current user available");
                }
            };
            failedTask.run();
            return failedTask;
        }
        return searchUsers(query, currentUserId, page, size);
    }

    /**
     * Check if the current search is a duplicate of the last search
     */
    public static boolean isDuplicateSearch(String query, Long currentUserId) {
        String lastQuery = lastSearchQuery.get();
        Long lastUserId = lastSearchUserId.get();
        
        return query.equals(lastQuery) && currentUserId.equals(lastUserId);
    }

    /**
     * Update the last search cache
     */
    public static void updateLastSearch(String query, Long currentUserId) {
        lastSearchQuery.set(query);
        lastSearchUserId.set(currentUserId);
    }

    /**
     * Clear the search cache
     */
    public static void clearSearchCache() {
        lastSearchQuery.set(null);
        lastSearchUserId.set(null);
        searchCache.clear();
        log.debug("Search cache cleared");
    }

    /**
     * Clear expired cache entries
     */
    public static void clearExpiredCache() {
        searchCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        log.debug("Expired cache entries cleared");
    }

    /**
     * Execute search asynchronously with better performance
     */
    public static CompletableFuture<PagedSearchResults> searchUsersAsync(String query, Long currentUserId, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            Task<PagedSearchResults> task = searchUsers(query, currentUserId, page, size);
            task.run();
            try {
                return task.get();
            } catch (Exception e) {
                log.error("Async search failed: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
}

