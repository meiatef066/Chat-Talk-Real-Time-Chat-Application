package com.system.chattalk_serverside.service.Connections;

import com.system.chattalk_serverside.dto.ContactDto.FriendRequestResponse;
import com.system.chattalk_serverside.dto.ContactDto.PendingFriendRequestDto;
import com.system.chattalk_serverside.dto.Entity.UserDTO;

import java.util.List;

public interface ContactService {
    FriendRequestResponse sendFriendRequest( String email);
    List<UserDTO> getFriends();
    List<PendingFriendRequestDto> getPendingRequests();
    void acceptRequest( Long requestId);
    void rejectRequest( Long requestId);

}
