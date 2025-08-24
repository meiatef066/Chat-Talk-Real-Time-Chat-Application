package com.system.chattalkdesktop.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.Dto.UpdateProfileRequest;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ApiProfileService {
    private static final String BASE_URL = "http://localhost:8080/api";
    static ObjectMapper mapper = JacksonConfig.getObjectMapper();
    static HttpClient client = HttpClient.newHttpClient();

    // Remove static TOKEN and get it dynamically
    private static String getToken() {
        return "Bearer " + SessionManager.getInstance().getToken();
    }

    public static Task<UserDTO> getProfile() {
        return new Task<>() {
            @Override
            protected UserDTO call() {
                try {
                    String token = getToken();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/profile/me"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                        if (apiResponse.getData() != null && apiResponse.getData().containsKey("user")) {
                            System.out.println("profile : " + apiResponse.getData().get("user"));

                            // Convert the user data to UserDTO
                            Object userData = apiResponse.getData().get("user");
                            if (userData instanceof String) {
                                return mapper.readValue((String) userData, UserDTO.class);
                            } else {
                                return mapper.convertValue(userData, UserDTO.class);
                            }
                        } else {
                            System.out.println("no profile data: " + apiResponse.getData());
                            NotificationServiceImpl.getInstance()
                                    .showErrorNotification("Fetch profile failed", apiResponse.getMessage());
                            return null;
                        }
                    } else {
                        System.err.println("Failed to fetch profile. Status: " + response.statusCode());
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Fetch profile failed", "HTTP " + response.statusCode());
                        return null;
                    }

                } catch (Exception e) {
                    System.err.println("Error in get profile data: " + e.getMessage());
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Error", "Failed to fetch profile: " + e.getMessage());
                    return null;
                }
            }
        };
    }

    public static Task<UserDTO> updateProfile( UpdateProfileRequest request) {
        return new Task<>() {
            @Override
            protected UserDTO call() {
                try {
                    // Convert request to JSON
                    String jsonBody = mapper.writeValueAsString(request);
                    System.out.println("Sending JSON request: " + jsonBody);

                    // Build HTTP request
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/profile/me"))
                            .header("Authorization", getToken())
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                        NotificationServiceImpl.getInstance()
                                .showSuccessNotification("Profile updated", apiResponse.getMessage());

                        // Return updated user data
                        if (apiResponse.getData() != null && apiResponse.getData().containsKey("user")) {
                            Object userData = apiResponse.getData().get("user");
                            if (userData instanceof String) {
                                return mapper.readValue((String) userData, UserDTO.class);
                            } else {
                                return mapper.convertValue(userData, UserDTO.class);
                            }
                        }
                    } else {
                        System.err.println("Failed to update profile. Status: " + response.statusCode());
                        String errorMessage = "HTTP " + response.statusCode();
                        try {
                            ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                            errorMessage = apiResponse.getMessage();
                        } catch (Exception e) {
                            // Ignore parsing errors for error response
                        }
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Failed", errorMessage);
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

    public static Task<UserDTO> updateProfilePicture( File profilePicture) {
        return new Task<>() {
            @Override
            protected UserDTO call() {
                try {
                    String boundary = "----JavaClientFormBoundary" + System.currentTimeMillis();

                    // Build multipart body for image only
                    StringBuilder bodyBuilder = new StringBuilder();
                    byte[] fileBytes = Files.readAllBytes(profilePicture.toPath());

                    bodyBuilder.append("--").append(boundary).append("\r\n")
                            .append("Content-Disposition: form-data; name=\"profilePicture\"; filename=\"")
                            .append(profilePicture.getName()).append("\"\r\n")
                            .append("Content-Type: image/jpeg\r\n\r\n");

                    // Convert to byte array
                    byte[] bodyStart = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    byte[] bodyEnd = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

                    byte[] finalBody = new byte[bodyStart.length + fileBytes.length + bodyEnd.length];
                    System.arraycopy(bodyStart, 0, finalBody, 0, bodyStart.length);
                    System.arraycopy(fileBytes, 0, finalBody, bodyStart.length, fileBytes.length);
                    System.arraycopy(bodyEnd, 0, finalBody, bodyStart.length + fileBytes.length, bodyEnd.length);

                    // Debug logging
                    System.out.println("Sending profile picture update request with boundary: " + boundary);

                    // Build HTTP request
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/profile/me/picture"))
                            .header("Authorization", getToken())
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(finalBody))
                            .build();

                    HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                        NotificationServiceImpl.getInstance()
                                .showSuccessNotification("Profile picture updated", apiResponse.getMessage());

                        // Return updated user data
                        if (apiResponse.getData() != null && apiResponse.getData().containsKey("user")) {
                            Object userData = apiResponse.getData().get("user");
                            if (userData instanceof String) {
                                return mapper.readValue((String) userData, UserDTO.class);
                            } else {
                                return mapper.convertValue(userData, UserDTO.class);
                            }
                        }
                    } else {
                        System.err.println("Failed to update profile picture. Status: " + response.statusCode());
                        String errorMessage = "HTTP " + response.statusCode();
                        try {
                            ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                            errorMessage = apiResponse.getMessage();
                        } catch (Exception e) {
                            // Ignore parsing errors for error response
                        }
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Failed", errorMessage);
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

    public static Task<Void> changePassword(String email, String oldPassword, String newPassword) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    String requestBody = String.format("email=%s&oldPassword=%s&newPassword=%s",
                            email, oldPassword, newPassword);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/profile/me/change-password"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Authorization", getToken())
                            .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        NotificationServiceImpl.getInstance()
                                .showSuccessNotification("Success", "Password changed successfully");
                    } else {
                        NotificationServiceImpl.getInstance()
                                .showErrorNotification("Failed", "Failed to change password. Status: " + response.statusCode());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationServiceImpl.getInstance()
                            .showErrorNotification("Failed", "Error changing password: " + e.getMessage());
                }
                return null;
            }
        };
    }
}