package com.system.chattalk_serverside.service.Message;

import com.system.chattalk_serverside.dto.ConversationDTO;
import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import com.system.chattalk_serverside.enums.MessageType;
import com.system.chattalk_serverside.model.Chat;
import com.system.chattalk_serverside.model.Message;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.ChatRepository;
import com.system.chattalk_serverside.repository.MessageRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl( MessageRepository messageRepository, ChatRepository chatRepository, UserRepository userRepository ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<MessageDTO> getMessagesHistory( Long conversationId, int page, int size ) {
        Chat chat = validateConversation(conversationId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Message> pageResult = messageRepository.findByChat_IdOrderByCreatedAtDesc(chat.getId(), pageable);
        return pageResult.getContent()
                .stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationDTO> getUnreadMessagesCount() {
        Long userId = getAuthenticatedUserId();
        List<Object[]> results = messageRepository.countUnreadMessages(userId);

        return results.stream()
                .map(row -> {
                    Long chatId = (Long) row[0];
                    Long unreadCount = (Long) row[1];
                    String lastMessage = (String) row[2];

                    return ConversationDTO.builder()
                            .conversationId(chatId)
                            .unreadCount(unreadCount)
                            .lastMessage(lastMessage)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void markConversationAsRead( Long chatId, Long userId ) {
        Chat chat = validateConversation(chatId);
        if (!isUserInChat(chat.getId(), userId)) {
            throw new IllegalArgumentException("User is not a participant in this chat");
        }
        messageRepository.markConversationAsReadForUser(chatId, userId);
    }

    @Override
    public List<ConversationDTO> getConversations() {
        Long userId = getAuthenticatedUserId();
        return chatRepository.findChatsByUserId(userId).stream()
                .map(c -> ConversationDTO.builder()
                        .conversationId(c.getId())
                        .lastMessage(c.getLastMessage())
                        .unreadCount(messageRepository.countUnreadInChatForUser(c.getId(), userId))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadMessageCount( Long conversationId, Long userId ) {
        Chat chat = validateConversation(conversationId);
        if (!isUserInChat(chat.getId(), userId)) {
            throw new IllegalArgumentException("User is not a participant in this chat");
        }
        return messageRepository.countUnreadInChatForUser(conversationId, userId);
    }

    @Override
    public MessageDTO sendMessage( MessageDTO messageDTO ) {
        if (messageDTO == null || messageDTO.getChatId() == null || messageDTO.getContent() == null) {
            throw new IllegalArgumentException("Invalid message payload");
        }
        Chat chat = validateConversation(messageDTO.getChatId());
        Long senderId = getAuthenticatedUserId();
        if (!isUserInChat(chat.getId(), senderId)) {
            throw new IllegalArgumentException("User is not a participant in this chat");
        }
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(messageDTO.getContent())
                .messageType(MessageType.TEXT)
                .isRead(false)
                .isEdited(false)
                .build();
        Message saved = messageRepository.save(message);

        chat.setLastMessage(saved.getContent());
        // updatedAt managed by @UpdateTimestamp
        // persist chat lastMessage change
        // chatRepository.save(chat); // optional if dirty tracking persists

        return toMessageDto(saved);
    }

    @Override
    public MessageDTO getLastMessage( Long conversationId ) {
        Chat chat = validateConversation(conversationId);
        Message last = messageRepository.findTopByChat_IdOrderByCreatedAtDesc(chat.getId());
        return last == null ? null : toMessageDto(last);
    }

    @Override
    public void deleteMessage( Long chatId, Long messageId, Long userId, boolean forEveryone ) {
        Chat chat = validateConversation(chatId);
        if (!isUserInChat(chat.getId(), userId)) {
            throw new IllegalArgumentException("User is not a participant in this chat");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!message.getChat().getId().equals(chatId)) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }
        boolean isSender = message.getSender().getId().equals(userId);
        boolean isOwner = chat.getCreatedBy() != null && chat.getCreatedBy().getId().equals(userId);
        if (forEveryone && !(isSender || isOwner)) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to delete for everyone");
        }
        if (!forEveryone && !isSender) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to delete others' messages");
        }
        messageRepository.deleteById(messageId);
    }

    @Override
    public MessageDTO editMessage( Long chatId, Long messageId, String newContent, Long userId ) {
        Chat chat = validateConversation(chatId);
        if (!isUserInChat(chat.getId(), userId)) {
            throw new IllegalArgumentException("User is not a participant in this chat");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!message.getChat().getId().equals(chatId)) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }
        if (!message.getSender().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to edit this message");
        }
        message.setContent(newContent);
        message.setIsEdited(true);
        Message saved = messageRepository.save(message);
        return toMessageDto(saved);
    }

    @Override
    public MessageDTO sendMediaMessage( Long conversationId, Long senderId, File file ) {
        return null;
    }

    // Helper
    private Chat validateConversation( Long conversationId ) {
        Optional<Chat> chat = chatRepository.findById(conversationId);
        if (chat.isEmpty()) {
            throw new RuntimeException("Invalid conversation");
        }
        return chat.get();
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            return u.getId();
        }
        throw new RuntimeException("Invalid principal for User Credentials");
    }

    private MessageDTO toMessageDto( Message message ) {
        return MessageDTO.builder().messageId(message.getId()).chatId(message.getChat().getId()).messageType(message.getMessageType().name()).content(message.getContent()).isRead(message.getIsRead()).senderId(message.getSender().getId()).timestamp(message.getCreatedAt()).chatName(message.getChat().getName()).build();
    }

    private boolean isUserInChat(Long chatId, Long userId) {
        return chatRepository.isUserInChat(chatId, userId);
    }
}
