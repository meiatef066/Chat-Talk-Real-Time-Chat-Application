package com.system.chattalkdesktop.AuthService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.Dto.ApiResponse;
import com.system.chattalkdesktop.Dto.AuthDto.*;
import com.system.chattalkdesktop.NotificationService.NotificationServiceImpl;
import com.system.chattalkdesktop.utils.JacksonConfig;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiAuthService {
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    static ObjectMapper mapper = JacksonConfig.getObjectMapper();
    static HttpClient client = HttpClient.newHttpClient();

    public static Task<AuthResponse> signUpApi( RegisterRequest requestBody) {
        return new Task<>() {
            @Override
            protected AuthResponse call() {
                try {
                    String jsonBody = mapper.writeValueAsString(requestBody);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());

                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("user")) {
                        return mapper.convertValue(apiResponse.getData().get("user"), AuthResponse.class);
                    } else {
                        NotificationServiceImpl.getInstance().showErrorNotification(
                                "Registration Failed",
                                apiResponse.getMessage()
                        );
                        return null;
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Task<Void> verifyEmail(String email, String code) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    VerifyRequest requester = new VerifyRequest(email, code);
                    String jsonBody = mapper.writeValueAsString(requester);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/verify-email"))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
    }

    public static Task<AuthResponse> loginApi( LoginRequest requestBody) {
        return new Task<>() {
            @Override
            protected AuthResponse call() {
                try {
                    String jsonBody = mapper.writeValueAsString(requestBody);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());

                    ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);

                    if (apiResponse.getData() != null && apiResponse.getData().containsKey("user")) {
                        return mapper.convertValue(apiResponse.getData().get("user"), AuthResponse.class);
                    } else {
                        NotificationServiceImpl.getInstance().showErrorNotification(
                                "Login Failed",
                                apiResponse.getMessage()
                        );
                        return null;
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Task<Void> forgetPassword(String email) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/forget-password?email=" + email))
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                System.out.println(apiResponse);

                if (!(response.statusCode() >= 200 && response.statusCode() < 300)) {
                    NotificationServiceImpl.getInstance().showErrorNotification(
                            "Error 🎶",
                            apiResponse.getMessage()
                    );
                }
                return null;
            }
        };
    }

    public static Task<Void> resetPassword( ResetPasswordRequest requestBody) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                String jsonBody = mapper.writeValueAsString(requestBody);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json")
                        .uri(URI.create(BASE_URL + "/reset-password"))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                ApiResponse apiResponse = mapper.readValue(response.body(), ApiResponse.class);
                System.out.println(response.body());

                if (!(response.statusCode() >= 200 && response.statusCode() < 300)) {
                    NotificationServiceImpl.getInstance().showErrorNotification(
                            "ERROR 🚩",
                            apiResponse.getMessage()
                    );
                }
                return null;
            }
        };
    }
}
