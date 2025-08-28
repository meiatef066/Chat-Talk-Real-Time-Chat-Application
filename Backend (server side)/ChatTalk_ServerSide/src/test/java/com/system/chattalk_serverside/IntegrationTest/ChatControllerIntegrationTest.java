package com.system.chattalk_serverside.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalk_serverside.enums.ChatType;
import com.system.chattalk_serverside.model.Chat;
import com.system.chattalk_serverside.model.ChatParticipation;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatControllerIntegrationTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER2_EMAIL = "test2@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.system.chattalk_serverside.repository.UserRepository userRepository;

    @Autowired
    private com.system.chattalk_serverside.repository.ChatRepository chatRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private ObjectMapper objectMapper;
    private User testUser;
    private User testUser2;
    private Chat testChat;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Create test users
        testUser = User.builder()
                .email(TEST_USER_EMAIL)
                .firstName("Test")
                .lastName("User")
                .password("password")
                .build();

        testUser2 = User.builder()
                .email(TEST_USER2_EMAIL)
                .firstName("Test2")
                .lastName("User")
                .password("password")
                .build();

        // Save users to get IDs
        testUser = userRepository.save(testUser);
        testUser2 = userRepository.save(testUser2);

        // Create friendship between users (required for chat creation)
        FriendRequest friendRequest = FriendRequest.builder()
                .sender(testUser)
                .receiver(testUser2)
                .status(FriendRequest.RequestStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .respondedAt(LocalDateTime.now())
                .build();
        friendRequestRepository.save(friendRequest);

        // Create test chat with proper initialization
        testChat = Chat.builder()
                .name("Test Chat")
                .chatType(ChatType.PRIVATE)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messages(new ArrayList<>()) // Initialize empty messages list
                .participants(new HashSet<>()) // Initialize empty participants set
                .build();

        // Save chat to get ID
        testChat = chatRepository.save(testChat);

        // Create and save chat participations
        ChatParticipation participation1 = ChatParticipation.builder()
                .chat(testChat)
                .user(testUser)
                .role(ChatParticipation.ParticipationRole.MEMBER)
                .status(ChatParticipation.ParticipationStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        ChatParticipation participation2 = ChatParticipation.builder()
                .chat(testChat)
                .user(testUser2)
                .role(ChatParticipation.ParticipationRole.MEMBER)
                .status(ChatParticipation.ParticipationStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        // Add participations to chat
        testChat.getParticipants().add(participation1);
        testChat.getParticipants().add(participation2);
        
        // Save the updated chat
        testChat = chatRepository.save(testChat);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void createPrivateChat_returns200() throws Exception {
        mockMvc.perform(post("/api/chats/private")
                        .param("email2", TEST_USER2_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNumber()); // Returns Long chat ID
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void createPrivateChat_userNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/chats/private")
                        .param("email2", "nonexistent@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
                
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getChatById_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/" + testChat.getId())
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value(testChat.getId()))
                .andExpect(jsonPath("$.chatName").value("Test Chat"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getChatById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/chats/999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void searchChatsByName_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/search")
                        .param("q", "Test")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }


    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getUserPrivateChats_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/private")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getUserAllChats_returns200() throws Exception {
        mockMvc.perform(get("/api/chats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getChatParticipants_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/" + testChat.getId() + "/participants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getChatMessageCount_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/" + testChat.getId() + "/message-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getUserChatCount_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void privateChatExists_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/exists")
                        .param("email2", TEST_USER2_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void deletePrivateChat_returns200() throws Exception {
        mockMvc.perform(delete("/api/chats/" + testChat.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void getRecentChats_returns200() throws Exception {
        mockMvc.perform(get("/api/chats/recent")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }
}
