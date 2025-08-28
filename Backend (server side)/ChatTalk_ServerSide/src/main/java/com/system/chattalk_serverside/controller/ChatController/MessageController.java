package com.system.chattalk_serverside.controller.ChatController;

import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import com.system.chattalk_serverside.service.Message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.system.chattalk_serverside.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Chats API → /api/chats
 * <p>
 * GET /api/chats → list user’s conversations
 * <p>
 * GET /api/chats/{chatId} → chat details (maybe participants, last message, etc.)
 * <p>
 * GET /api/chats/{chatId}/messages → paginated messages
 * <p>
 * POST /api/chats/{chatId}/messages → send message in chat
 * <p>
 * PUT /api/chats/{chatId}/messages/{messageId} → edit message
 * <p>
 * DELETE /api/chats/{chatId}/messages/{messageId} → delete message
 * <p>
 * POST /api/chats/{chatId}/read → mark chat as read
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Message Management", description = "Message operations within chats")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Get paginated messages", description = "Retrieves paginated messages for a chat (newest first)", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = {@Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"), @Parameter(name = "page", description = "Page number (0-based)", required = false, example = "0"), @Parameter(name = "size", description = "Page size", required = false, example = "20")})
    public ResponseEntity<List<MessageDTO>> getMessages( @PathVariable Long chatId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size ) {
        return ResponseEntity.ok(messageService.getMessagesHistory(chatId, page, size));
    }

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Send a message", description = "Sends a message in the specified chat", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"))
    public ResponseEntity<MessageDTO> sendMessage( @PathVariable Long chatId, @RequestBody MessageDTO dto ) {
        dto.setChatId(chatId);
        MessageDTO created = messageService.sendMessage(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{chatId}/read")
    @Operation(summary = "Mark chat as read", description = "Marks all messages as read for the current user in the specified chat", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"))
    public ResponseEntity<Void> markRead( @PathVariable Long chatId ) {
        Long userId = getCurrentUserId();
        messageService.markConversationAsRead(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/unread")
    @Operation(summary = "Get unread count", description = "Returns the number of unread messages for the current user in the specified chat", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"))
    public ResponseEntity<Long> unreadCount( @PathVariable Long chatId ) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(messageService.getUnreadMessageCount(chatId, userId));
    }

    @PatchMapping("/{chatId}/messages/{messageId}")
    @Operation(summary = "Edit a message", description = "Edits a message sent by the current user", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = {@Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"), @Parameter(name = "messageId", description = "ID of the message", required = true, example = "456"), @Parameter(name = "content", description = "New message content", required = true, example = "Updated text")})
    public ResponseEntity<MessageDTO> edit( @PathVariable Long chatId, @PathVariable Long messageId, @RequestParam String content ) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(messageService.editMessage(chatId, messageId, content, userId));
    }

    @DeleteMapping("/{chatId}/messages/{messageId}")
    @Operation(summary = "Delete a message", description = "Deletes a message. Users can delete their own messages, optionally for everyone.", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = {@Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"), @Parameter(name = "messageId", description = "ID of the message", required = true, example = "456"), @Parameter(name = "forEveryone", description = "Delete for everyone if permitted", required = false, example = "false")})
    public ResponseEntity<Void> delete( @PathVariable Long chatId, @PathVariable Long messageId, @RequestParam(defaultValue = "false") boolean forEveryone ) {
        Long userId = getCurrentUserId();
        messageService.deleteMessage(chatId, messageId, userId, forEveryone);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            return u.getId();
        }
        throw new RuntimeException("Invalid principal for User Credentials");
    }
}