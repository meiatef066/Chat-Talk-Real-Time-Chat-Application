package com.system.chattalkdesktop.Dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Data
@Builder
public class ApiResponse {
    private String timeStamp;  // Keep as String to handle LocalDateTime from backend
    private int statusCode;
    private String status;     // Keep as String to handle HttpStatus enum from backend
    private String message;
    private String error;
    private String path;
    private String developerMessage;
    private Map<?, ?> data;
    
    // Jackson constructor for deserialization
    @JsonCreator
    public ApiResponse(
            @JsonProperty("timeStamp") String timeStamp,
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("status") String status,
            @JsonProperty("message") String message,
            @JsonProperty("error") String error,
            @JsonProperty("path") String path,
            @JsonProperty("developerMessage") String developerMessage,
            @JsonProperty("data") Map<?, ?> data) {
        this.timeStamp = timeStamp;
        this.statusCode = statusCode;
        this.status = status;
        this.message = message;
        this.error = error;
        this.path = path;
        this.developerMessage = developerMessage;
        this.data = data;
    }
    
    // Default constructor for Jackson
    public ApiResponse() {
    }
} 