package com.system.chattalk_serverside.dto.AuthDto;

import com.system.chattalk_serverside.dto.Entity.UserDTO;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String expiresIn;
    private String tokenType;
    private UserDTO userDTO;
}