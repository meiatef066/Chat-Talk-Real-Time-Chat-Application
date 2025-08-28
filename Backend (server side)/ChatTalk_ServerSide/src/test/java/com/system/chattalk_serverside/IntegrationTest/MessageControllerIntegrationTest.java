package com.system.chattalk_serverside.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalk_serverside.dto.Entity.MessageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMessages_shouldReturnForbidden_whenNoAuth() throws Exception {
        mockMvc.perform(get("/api/chats/1/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessage_shouldValidatePayload() throws Exception {
        MessageDTO dto = MessageDTO.builder().content("hello").build();
        mockMvc.perform(post("/api/chats/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}