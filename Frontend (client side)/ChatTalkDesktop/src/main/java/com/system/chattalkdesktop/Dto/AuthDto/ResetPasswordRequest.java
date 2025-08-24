package com.system.chattalkdesktop.Dto.AuthDto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String newPassword;
    
    // Jackson constructor for deserialization
    @JsonCreator
    public ResetPasswordRequest(
            @JsonProperty("email") String email,
            @JsonProperty("code") String code,
            @JsonProperty("newPassword") String newPassword) {
        this.email = email;
        this.code = code;
        this.newPassword = newPassword;
    }
    
    // Default constructor for Jackson
    public ResetPasswordRequest() {
    }
}