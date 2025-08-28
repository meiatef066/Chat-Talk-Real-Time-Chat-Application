package com.system.chattalk_serverside.dto.Entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Builder
//@NoArgsConstructor
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private Boolean isOnline;
    private Boolean isVerified;
    private String phoneNumber;
    private String bio;
    private String gender;
    private Date dateOfBirth;
    private LocalDateTime lastSeen;
}
