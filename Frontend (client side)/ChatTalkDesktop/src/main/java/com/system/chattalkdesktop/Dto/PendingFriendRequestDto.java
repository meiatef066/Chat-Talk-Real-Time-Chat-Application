package com.system.chattalkdesktop.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PendingFriendRequestDto {
    private Long requestId;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private Boolean isOnline;
    private String bio;
    private String gender;
    private Date dateOfBirth;
    private String requestDate;
}
