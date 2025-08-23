package com.system.chattalk_serverside.dto.ContactDto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
    private LocalDateTime dateOfBirth;
    private LocalDateTime requestDate;
}