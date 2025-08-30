package com.system.chattalkdesktop.SearchService;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class SearchUserResultDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;
    private Boolean isVerified;
    private String relationshipStatus; // ACCEPTED, BLOCKED, PENDING, NONE

}

