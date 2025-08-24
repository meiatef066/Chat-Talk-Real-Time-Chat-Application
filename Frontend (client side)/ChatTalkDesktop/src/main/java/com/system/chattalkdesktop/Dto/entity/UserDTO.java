package com.system.chattalkdesktop.Dto.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Builder
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
    // Jackson constructor for deserialization
    @JsonCreator
    public UserDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("username") String username,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("email") String email,
            @JsonProperty("profilePictureUrl") String profilePictureUrl,
            @JsonProperty("isOnline") Boolean isOnline,
            @JsonProperty("isVerified") Boolean isVerified,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("bio") String bio,
            @JsonProperty("gender") String gender,
            @JsonProperty("dateOfBirth") Date dateOfBirth,
      @JsonProperty("lastSeen")
                    LocalDateTime lastSeen)
    {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.isOnline = isOnline;
        this.isVerified = isVerified;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.lastSeen=lastSeen;
    }

}
