package com.system.chattalk_serverside.controller.contactController;

import com.system.chattalk_serverside.dto.Entity.ChatDto;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.Chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat Management", description = "Chat creation and management APIs")
public class ChatController {
    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/private")
    @Operation(
            summary = "Create or get private chat",
            description = "Creates a new private chat between the current user and another user, or returns existing chat ID if one already exists.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "email2",
                    description = "Email address of the other user to chat with",
                    required = true,
                    example = "jane.doe@example.com"
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Chat created or retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Chat Response",
                                    value = "123"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found with provided email"
            )
    })
    public ResponseEntity<Long> getOrCreatePrivateChat(@RequestParam String email2) {
        String email1 = SecurityContextHolder.getContext().getAuthentication().getName();
        Long chatId = chatService.GetPrivateChat(email1, email2);
        return ResponseEntity.ok(chatId);
    }

    @GetMapping("/private")
    @Operation(
            summary = "Get user's private chats",
            description = "Retrieves all private chats for the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<ChatDto>> getUserPrivateChats() {
        List<ChatDto> chats = chatService.getUserPrivateChats();
        return ResponseEntity.ok(chats);
    }

    @GetMapping
    @Operation(
            summary = "Get all user's chats",
            description = "Retrieves all chats (private and group) for the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<ChatDto>> getUserAllChats() {
        List<ChatDto> chats = chatService.getUserAllChats();
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    @Operation(
            summary = "Get chat by ID",
            description = "Retrieves a specific chat by its ID",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatId",
                    description = "ID of the chat to retrieve",
                    required = true,
                    example = "123"
            )
    )
    public ResponseEntity<ChatDto> getChatById(@PathVariable Long chatId) {
        Optional<ChatDto> chat = chatService.getChatById(chatId);
        return chat.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{chatId}/name")
    @Operation(
            summary = "Update chat name",
            description = "Updates the name of a chat (only for participants)",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = {
                @Parameter(name = "chatId", description = "ID of the chat", required = true, example = "123"),
                @Parameter(name = "newName", description = "New name for the chat", required = true, example = "Updated Chat Name")
            }
    )
    public ResponseEntity<ChatDto> updateChatName(
            @PathVariable Long chatId,
            @RequestParam String newName) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        ChatDto updatedChat = chatService.updateChatName(chatId, newName, userEmail);
        return ResponseEntity.ok(updatedChat);
    }

    @DeleteMapping("/{chatId}")
    @Operation(
            summary = "Delete private chat",
            description = "Deletes a private chat and all its messages (only for participants)",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatId",
                    description = "ID of the private chat to delete",
                    required = true,
                    example = "123"
            )
    )
    public ResponseEntity<Void> deletePrivateChat(@PathVariable Long chatId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        chatService.deletePrivateChat(chatId, userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search chats by name",
            description = "Searches for chats by name (case-insensitive)",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = {
                @Parameter(name = "q", description = "Search term", required = false, example = "work"),
                @Parameter(name = "page", description = "Page number (0-based)", required = false, example = "0"),
                @Parameter(name = "size", description = "Page size", required = false, example = "20")
            }
    )
    public ResponseEntity<Page<ChatDto>> searchChatsByName(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatDto> chats = chatService.searchChatsByName(q, pageable);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/type/{chatType}")
    @Operation(
            summary = "Get chats by type",
            description = "Retrieves all chats of a specific type for the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatType",
                    description = "Type of chat (PRIVATE, GROUP, etc.)",
                    required = true,
                    example = "PRIVATE"
            )
    )
    public ResponseEntity<List<ChatDto>> getChatsByType(@PathVariable String chatType) {
        List<ChatDto> chats = chatService.getChatsByType(chatType);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get recent chats",
            description = "Retrieves the most recent chats for the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "limit",
                    description = "Maximum number of chats to return",
                    required = false,
                    example = "10"
            )
    )
    public ResponseEntity<List<ChatDto>> getRecentChats(
            @RequestParam(defaultValue = "10") int limit) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ChatDto> chats = chatService.getRecentChats(userEmail, limit);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}/participants")
    @Operation(
            summary = "Get chat participants",
            description = "Retrieves all participants in a specific chat",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatId",
                    description = "ID of the chat",
                    required = true,
                    example = "123"
            )
    )
    public ResponseEntity<List<String>> getChatParticipants(@PathVariable Long chatId) {
        List<String> participants = chatService.getChatParticipants(chatId);
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/{chatId}/leave")
    @Operation(
            summary = "Leave chat",
            description = "Leaves a chat (marks participation as LEFT)",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatId",
                    description = "ID of the chat to leave",
                    required = true,
                    example = "123"
            )
    )
    public ResponseEntity<Void> leaveChat(@PathVariable Long chatId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        chatService.leaveChat(chatId, userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatId}/message-count")
    @Operation(
            summary = "Get chat message count",
            description = "Retrieves the total number of messages in a chat",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "chatId",
                    description = "ID of the chat",
                    required = true,
                    example = "123"
            )
    )
    public ResponseEntity<Long> getChatMessageCount(@PathVariable Long chatId) {
        Long messageCount = chatService.getChatMessageCount(chatId);
        return ResponseEntity.ok(messageCount);
    }

    @GetMapping("/count")
    @Operation(
            summary = "Get user's chat count",
            description = "Retrieves the total number of chats for the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Long> getUserChatCount() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Long chatCount = chatService.getUserChatCount(userEmail);
        return ResponseEntity.ok(chatCount);
    }

    @GetMapping("/exists")
    @Operation(
            summary = "Check if private chat exists",
            description = "Checks if a private chat exists between two users",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = @Parameter(
                    name = "email2",
                    description = "Email of the other user",
                    required = true,
                    example = "jane.doe@example.com"
            )
    )
    public ResponseEntity<Boolean> privateChatExists(@RequestParam String email2) {
        String email1 = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean exists = chatService.privateChatExists(email1, email2);
        return ResponseEntity.ok(exists);
    }
}
