package com.system.chattalk_serverside.dto.ContactDto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FriendRequestResponse {
    private Long requestId;
    private String sender;
    private String receiver;
    private String status;
}
