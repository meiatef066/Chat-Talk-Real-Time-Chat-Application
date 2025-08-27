package com.system.chattalk_serverside.mapper;

import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.getIsOnline())
                .isVerified(user.getIsVerified())
                .lastSeen(user.getLastSeen())
                .build();
    }
}