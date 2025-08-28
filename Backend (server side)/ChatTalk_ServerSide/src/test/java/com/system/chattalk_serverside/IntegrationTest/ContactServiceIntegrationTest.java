
package com.system.chattalk_serverside.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.Connections.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class ContactServiceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContactService contactService;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendRequestRepository friendRequestRepository;

    private static final String TEST_USER_EMAIL = "john@example.com";
    private static final String TEST_USER2_EMAIL = "jane@example.com";
    
    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // Create and save test users
        testUser = User.builder()
                .email(TEST_USER_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .username("johndoe")
                .isVerified(true)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser2 = User.builder()
                .email(TEST_USER2_EMAIL)
                .firstName("Jane")
                .lastName("Smith")
                .password("password123")
                .username("janesmith")
                .isVerified(true)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = userRepository.save(testUser);
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void sendFriendRequest_returns200() throws Exception {
        mockMvc.perform(post("/api/contacts/requests")
                        .param("receiverEmail", TEST_USER2_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @WithMockUser(username = TEST_USER2_EMAIL)
    void acceptFriendRequest_returns200() throws Exception {
        // Step 1: Send friend request as testUser
        mockMvc.perform(post("/api/contacts/requests")
                        .with(user(TEST_USER_EMAIL))  // sender
                        .param("receiverEmail", TEST_USER2_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Step 2: Get the actual request ID
        FriendRequest request = friendRequestRepository.findBySenderAndReceiverAndStatus(
                testUser, testUser2, FriendRequest.RequestStatus.PENDING).orElseThrow();

        // Step 3: Accept as testUser2
        mockMvc.perform(post("/api/contacts/requests/" + request.getId() + "/accept")
                        .with(user(TEST_USER2_EMAIL)) // receiver
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL) // sender
    void rejectFriendRequest_returns200() throws Exception {
        // Send friend request from testUser to testUser2
        mockMvc.perform(post("/api/contacts/requests")
                        .param("receiverEmail", TEST_USER2_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Get the actual request ID
        FriendRequest request = friendRequestRepository.findBySenderAndReceiverAndStatus(
                testUser, testUser2, FriendRequest.RequestStatus.PENDING).get();

        // Now reject it as testUser2
        mockMvc.perform(post("/api/contacts/requests/" + request.getId() + "/reject")
                        .with(user(TEST_USER2_EMAIL)) // authenticated as receiver
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void sendFriendRequest_toSelf_returns400() throws Exception {
        mockMvc.perform(post("/api/contacts/requests")
                        .param("receiverEmail", TEST_USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void sendFriendRequest_toNonExistentUser_returns404() throws Exception {
        mockMvc.perform(post("/api/contacts/requests")
                        .param("receiverEmail", "nonexistent@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void acceptFriendRequest_notFound_returns500() throws Exception {
        mockMvc.perform(post("/api/contacts/requests/999/accept")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL)
    void rejectFriendRequest_notFound_returns500() throws Exception {
        mockMvc.perform(post("/api/contacts/requests/999/reject")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }
}
