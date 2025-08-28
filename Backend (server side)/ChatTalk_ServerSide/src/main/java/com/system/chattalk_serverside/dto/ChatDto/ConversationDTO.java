package com.system.chattalk_serverside.dto.ChatDto;

import com.system.chattalk_serverside.dto.Entity.UserDTO;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ConversationDTO {
    private Long conversationId;
    // If private chat
    private UserDTO otherUser;
    // Last message preview
    private String lastMessage;
    private Long unreadCount;
//   // If group chat
//    private String groupName;
//    private String groupAvatar;
}
