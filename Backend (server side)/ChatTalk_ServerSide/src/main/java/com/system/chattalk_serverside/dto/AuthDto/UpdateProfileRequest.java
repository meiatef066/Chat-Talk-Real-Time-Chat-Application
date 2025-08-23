package com.system.chattalk_serverside.dto.AuthDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileRequest {
    private String username;
    private String firstName;
    private String lastName;
    private Boolean isOnline;
    private String phoneNumber;
    private String bio;
    private String gender;
    private String dateOfBirth; // Changed from Date to Long to accept timestamp
}
