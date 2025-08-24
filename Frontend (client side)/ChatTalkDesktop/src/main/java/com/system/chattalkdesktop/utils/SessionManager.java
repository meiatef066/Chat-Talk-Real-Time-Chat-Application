package com.system.chattalkdesktop.utils;

import com.system.chattalkdesktop.Dto.AuthDto.AuthResponse;
import com.system.chattalkdesktop.Dto.entity.UserDTO;

public class SessionManager {
    private static SessionManager instance;
    private AuthResponse authResponse;
    private String token;
    private UserDTO currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void storeSession(AuthResponse authResponse) {
        this.authResponse = authResponse;
        this.token = authResponse.getToken();
        this.currentUser = authResponse.getUserDTO();
    }

    public void clearSession() {
        this.authResponse = null;
        this.token = null;
        this.currentUser = null;
    }

    public String getToken() {
        return token;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
