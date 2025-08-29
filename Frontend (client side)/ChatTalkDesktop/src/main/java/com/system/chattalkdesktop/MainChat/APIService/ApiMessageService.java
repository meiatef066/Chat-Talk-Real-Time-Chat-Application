package com.system.chattalkdesktop.MainChat.APIService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalkdesktop.utils.JacksonConfig;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.concurrent.Task;

import java.net.http.HttpClient;

public class ApiMessageService {
    private static final String BASE_URL = "http://localhost:8080/api/contacts";
    static ObjectMapper mapper = JacksonConfig.getObjectMapper();
    static HttpClient client = HttpClient.newHttpClient();

    // Remove static TOKEN and get it dynamically
    private static String getToken() {
        return "Bearer " + SessionManager.getInstance().getToken();
    }

    public Task<Void> getMessageHistory( Long ChatId){

        return null;
    }
    public Task<Void> deleteMessageHistory( Long ChatId){

        return null;
    }

}
