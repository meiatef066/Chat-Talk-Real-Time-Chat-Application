package com.system.chattalk_serverside.service.Chat;

import com.system.chattalk_serverside.dto.Entity.ChatDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;


public interface ChatService {
    // Core chat operations
    public Long GetPrivateChat(String email1, String email2);
    public List<Long> getAllUserPrivateChatId();
    public Page<ChatDto> searchChatsByName();
    
    // Enhanced chat management
    public List<ChatDto> getUserPrivateChats();
    public List<ChatDto> getUserAllChats();
    public Optional<ChatDto> getChatById(Long chatId);
    public ChatDto updateChatName(Long chatId, String newName, String userEmail);
    public void deletePrivateChat(Long chatId, String userEmail);
    public boolean privateChatExists(String email1, String email2);
    
    // Search and filtering
    public Page<ChatDto> searchChatsByName(String searchTerm, Pageable pageable);
    public List<ChatDto> getChatsByType(String chatType);
    public List<ChatDto> getRecentChats(String userEmail, int limit);
    
    // Chat participation management
    public boolean isUserInChat(Long chatId, String userEmail);
    public List<String> getChatParticipants(Long chatId);
    public void leaveChat(Long chatId, String userEmail);
    
    // Chat statistics
    public Long getChatMessageCount(Long chatId);
    public Long getUserChatCount(String userEmail);
}
