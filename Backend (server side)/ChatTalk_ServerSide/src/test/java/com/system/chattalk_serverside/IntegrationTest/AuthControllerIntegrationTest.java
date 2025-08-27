package com.system.chattalk_serverside.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.enums.Role;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.utils.AppMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

 import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AppMailService appMailService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "test+" + UUID.randomUUID() + "@example.com";

        registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("password");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("password");
    }

    @Test
    void registerUser_success() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.user.userDTO.email").value(testEmail));
    }

    @Test
    void registerUser_emailAlreadyExists_returnsBadRequest() throws Exception {
        // Ensure user with same email already exists
        User user = User.builder()
                .email(testEmail)
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid input: Email already in use"));
    }

    @Test
    void loginUser_success() throws Exception {
        // First register a user
        User user = User.builder()
                .email(testEmail)
                .password(passwordEncoder.encode("password"))
                .firstName("John")
                .lastName("Doe")
                .isVerified(true)
                .roles(List.of(Role.USER))
                .build();
        userRepository.save(user);

        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.user.userDTO.email").value(testEmail));
    }

    @Test
    void loginUser_invalidCredentials_returnsUnauthorized() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void forgetPassword_userExists_returnsOk() throws Exception {
        User user = User.builder()
                .email(testEmail)
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        ResultActions result = mockMvc.perform(post("/api/auth/forget-password")
                .param("email", testEmail));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset code sent to your email"));
    }

    @Test
    void forgetPassword_userNotFound_returnsNotFound() throws Exception {
        String missingEmail = "missing+" + UUID.randomUUID() + "@example.com";
        ResultActions result = mockMvc.perform(post("/api/auth/forget-password")
                .param("email", missingEmail));

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: "+missingEmail));
    }
}