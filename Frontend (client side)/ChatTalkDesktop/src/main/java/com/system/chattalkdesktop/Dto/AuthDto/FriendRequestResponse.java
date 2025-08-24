package com.system.chattalkdesktop.Dto.AuthDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private Long requestId;
    private String sender;
    private String receiver;
    private String status;
}
