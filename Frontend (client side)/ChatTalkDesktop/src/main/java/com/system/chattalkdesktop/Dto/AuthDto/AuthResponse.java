package com.system.chattalkdesktop.Dto.AuthDto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.system.chattalkdesktop.Dto.entity.UserDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String expiresIn;
    private String tokenType;
    private UserDTO userDTO;

    // Jackson constructor for deserialization
    @JsonCreator
    public AuthResponse(
            @JsonProperty("token") String token,
            @JsonProperty("refreshToken") String refreshToken,
            @JsonProperty("expiresIn") String expiresIn,
            @JsonProperty("tokenType") String tokenType,
            @JsonProperty("userDTO") UserDTO userDTO) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.userDTO = userDTO;
    }

    // Default constructor for Jackson
    public AuthResponse() {
    }
}
