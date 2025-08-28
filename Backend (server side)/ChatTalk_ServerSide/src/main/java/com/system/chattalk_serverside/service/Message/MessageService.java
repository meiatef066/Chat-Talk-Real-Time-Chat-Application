package com.system.chattalk_serverside.service.Message;

import com.system.chattalk_serverside.dto.ChatDto.SendMessageRequest;
import com.system.chattalk_serverside.dto.ChatDto.ConversationDTO;
import com.system.chattalk_serverside.dto.Entity.MessageDTO;

import java.io.File;
import java.util.List;

public interface MessageService {
    // core
    List<ConversationDTO> getConversations();
    //
    List<MessageDTO> getMessagesHistory( Long conversationId, int page, int size);
    List<ConversationDTO> getUnreadMessagesCount();
    void markConversationAsRead(Long conversationId, Long userId);

    Long getUnreadMessageCount(Long conversationId, Long userId);
    MessageDTO sendMessage( SendMessageRequest request);
    MessageDTO getLastMessage(Long conversationId);

    // Management
    void deleteMessage(Long chatId, Long messageId, Long userId, boolean forEveryone);
    MessageDTO editMessage(Long chatId, Long messageId, String newContent, Long userId);


    // Optional: media & reactions
    MessageDTO sendMediaMessage(Long conversationId, Long senderId, File file);
}
