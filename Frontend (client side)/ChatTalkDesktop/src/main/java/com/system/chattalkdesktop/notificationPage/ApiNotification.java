package com.system.chattalkdesktop.notificationPage;

import com.system.chattalkdesktop.Dto.entity.NotificationDTO;
import com.system.chattalkdesktop.utils.SessionManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;

public class ApiNotification {
    private static final String BASE_URL = "http://localhost:8080/api/notifications";
    private static ApiNotification instance;
    private final RestTemplate restTemplate;

    // Private constructor to prevent instantiation
    private ApiNotification() {
        // Configure RestTemplate to use Java's HttpClient which supports PATCH
        HttpClient httpClient = HttpClient.newHttpClient();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(factory);
    }

    // Singleton instance retrieval
    public static synchronized ApiNotification getInstance() {
        if (instance == null) {
            instance = new ApiNotification();
        }
        return instance;
    }
    
    /**
     * Get authentication headers with token
     */
    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isEmpty()) {
            headers.set("Authorization", "Bearer " + token);
        }
        headers.set("Content-Type", "application/json");
        return headers;
    }

    // Get all notifications
    public List<NotificationDTO> getNotifications() {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<NotificationDTO[]> response = restTemplate.exchange(
                BASE_URL, 
                HttpMethod.GET, 
                entity, 
                NotificationDTO[].class
            );
            
            NotificationDTO[] notifications = response.getBody();
            return notifications != null ? Arrays.asList(notifications) : List.of();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch notifications", e);
        }
    }

    // Get notifications with pagination
    public List<NotificationDTO> getNotifications(int page, int size) {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = BASE_URL + "?page=" + page + "&size=" + size;
            ResponseEntity<NotificationDTO[]> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                NotificationDTO[].class
            );
            
            NotificationDTO[] notifications = response.getBody();
            return notifications != null ? Arrays.asList(notifications) : List.of();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch notifications with pagination", e);
        }
    }

    // Get notification count
    public int getNotificationCount() {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<NotificationDTO[]> response = restTemplate.exchange(
                BASE_URL, 
                HttpMethod.GET, 
                entity, 
                NotificationDTO[].class
            );
            
            NotificationDTO[] notifications = response.getBody();
            return notifications != null ? notifications.length : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Delete all notifications
    public void deleteAllNotifications() {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(BASE_URL, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete all notifications", e);
        }
    }

    // Delete a specific notification by ID
    public void deleteNotification(Long id) {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(BASE_URL + "/" + id, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete notification with ID: " + id, e);
        }
    }

    // Mark a notification as read
    public NotificationDTO markAsRead(Long id) {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<NotificationDTO> response = restTemplate.exchange(
                BASE_URL + "/" + id + "/read", 
                HttpMethod.PATCH, 
                entity, 
                NotificationDTO.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to mark notification as read: " + id, e);
        }
    }
}