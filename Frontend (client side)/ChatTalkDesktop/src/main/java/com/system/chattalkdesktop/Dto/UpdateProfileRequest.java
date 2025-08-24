package com.system.chattalkdesktop.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String username;
    private String firstName;
    private String lastName;
    private Boolean isOnline;
    private String phoneNumber;
    private String bio;
    private String gender;
    private String dateOfBirth;
}