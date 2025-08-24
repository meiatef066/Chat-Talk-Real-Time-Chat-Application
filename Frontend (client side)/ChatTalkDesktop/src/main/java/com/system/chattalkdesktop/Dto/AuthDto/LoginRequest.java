package com.system.chattalkdesktop.Dto.AuthDto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequest {
    private String email;
    private String password;
    
    // Jackson constructor for deserialization
    @JsonCreator
    public LoginRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password) {
        this.email = email;
        this.password = password;
    }

    // Default constructor for Jackson
    public LoginRequest() {
    }
}
