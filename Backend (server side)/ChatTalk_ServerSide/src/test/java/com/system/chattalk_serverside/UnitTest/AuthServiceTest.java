

package com.system.chattalk_serverside.UnitTest;

import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.auth.AuthService;
import com.system.chattalk_serverside.service.VerificationService;
import com.system.chattalk_serverside.utils.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenManager tokenManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private VerificationService verificationService;

    @InjectMocks
    private AuthService authService;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .isOnline(false)
                .lastSeen(LocalDateTime.now())
                .build();
    }
    @Test
    void registerUser_success() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        when(tokenManager.generateToken(any(User.class))).thenReturn("accessToken");
        when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("refreshToken");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test@example.com", response.getUserDTO().getEmail());
        assertEquals("accessToken", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(verificationService, times(1)).sendVerificationCode("test@example.com");
    }
    @Test
    void registerUser_emailAlreadyExists_throwsException(){
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    void loginUser_success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenManager.generateToken(user)).thenReturn("accessToken");
        when(tokenManager.generateRefreshToken(user)).thenReturn("refreshToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertTrue(response.getUserDTO().getIsOnline());
        assertEquals("accessToken", response.getToken());
        verify(userRepository, times(1)).save(user);
    }
    @Test
    void loginUser_invalidCredentials_throwsBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    void refreshToken_success() {
        when(tokenManager.extractUsername("refreshToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenManager.validateToken("refreshToken", user)).thenReturn(true);
        when(tokenManager.generateToken(user)).thenReturn("newAccessToken");
        when(tokenManager.generateRefreshToken(user)).thenReturn("newRefreshToken");

        AuthResponse response = authService.refreshToken("refreshToken");

        assertNotNull(response);
        assertEquals("newAccessToken", response.getToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
    }
    @Test
    void verifyEmail_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verificationService.VerifyCode("test@example.com", "code")).thenReturn(true);

        authService.verifyEmail(new VerifyRequest("test@example.com", "code"));

        assertTrue(user.getIsVerified());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void forgetPassword_userExists_sendsVerificationCode() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        authService.forgetPassword("test@example.com");

        verify(verificationService, times(1)).sendVerificationCode("test@example.com");
    }
    @Test
    void forgetPassword_userNotFound_throwsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> authService.forgetPassword("test@example.com"));
    }

    @Test
    void resetPassword_success() {
        when(verificationService.VerifyCode("test@example.com", "code")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        ResetPasswordRequest request = ResetPasswordRequest
                .builder()
                .email("test@example.com")
                .code("code")
                .newPassword("newPassword")
                .build();
        authService.resetPassword(request);

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

}
