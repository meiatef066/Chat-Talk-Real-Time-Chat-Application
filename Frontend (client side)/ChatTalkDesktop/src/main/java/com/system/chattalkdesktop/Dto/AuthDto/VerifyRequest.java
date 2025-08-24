package com.system.chattalkdesktop.Dto.AuthDto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyRequest {
    private String email;
    private String code;
    
    // Jackson constructor for deserialization
    @JsonCreator
    public VerifyRequest(
            @JsonProperty("email") String email,
            @JsonProperty("code") String code) {
        this.email = email;
        this.code = code;
    }
    
    // Default constructor for Jackson
    public VerifyRequest() {
    }
}