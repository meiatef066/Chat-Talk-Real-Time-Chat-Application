package com.system.chattalk_serverside.UnitTest;

import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.VerificationService;
import com.system.chattalk_serverside.service.auth.AuthServiceImpl;
import com.system.chattalk_serverside.utils.TokenManager;
import com.system.chattalk_serverside.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Implementation Tests")
class AuthServiceImplTest {

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
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UserDTO testUserDTO;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private VerifyRequest verifyRequest;
    private ResetPasswordRequest resetPasswordRequest;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .isOnline(false)
                .lastSeen(testDateTime)
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isVerified(false)
                .isOnline(false)
                .lastSeen(testDateTime)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        verifyRequest = new VerifyRequest();
        verifyRequest.setEmail("test@example.com");
        verifyRequest.setCode("123456");

        resetPasswordRequest = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("newPassword123")
                .build();
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully with valid data")
        void register_Success() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tokenManager.generateToken(any(User.class))).thenReturn("accessToken");
            when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDTO);

            // Act
            AuthResponse response = authService.register(registerRequest);

            // Assert
            assertNotNull(response);
            assertEquals("accessToken", response.getToken());
            assertEquals("refreshToken", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(testUserDTO, response.getUserDTO());
            
            verify(userRepository).existsByEmail("test@example.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
            verify(verificationService).sendVerificationCode("test@example.com");
            verify(tokenManager).generateToken(any(User.class));
            verify(tokenManager).generateRefreshToken(any(User.class));
            verify(userMapper).toDto(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_EmailAlreadyExists_ThrowsIllegalArgumentException() {
            // Arrange
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, 
                () -> authService.register(registerRequest)
            );
            
            assertEquals("Email already in use", exception.getMessage());
            verify(userRepository).existsByEmail("test@example.com");
            verifyNoMoreInteractions(userRepository, passwordEncoder, tokenManager, verificationService, userMapper);
        }

        @Test
        @DisplayName("Should create user with correct default values")
        void register_CreatesUserWithCorrectDefaults() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });
            when(tokenManager.generateToken(any(User.class))).thenReturn("accessToken");
            when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDTO);

            // Act
            authService.register(registerRequest);

            // Assert
            verify(userRepository).save(argThat(user -> 
                user.getEmail().equals("test@example.com") &&
                user.getFirstName().equals("Test") &&
                user.getLastName().equals("User") &&
                user.getStatus() == UserStatus.ACTIVE &&
                !user.getIsVerified() &&
                !user.getIsOnline() &&
                user.getLastSeen() != null
            ));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void login_Success() {
            // Arrange
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tokenManager.generateToken(any(User.class))).thenReturn("accessToken");
            when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDTO);

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals("accessToken", response.getToken());
            assertEquals("refreshToken", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(testUserDTO, response.getUserDTO());
            
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).save(argThat(user -> 
                user.getIsOnline() && 
                user.getLastSeen() != null
            ));
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid credentials")
        void login_InvalidCredentials_ThrowsBadCredentialsException() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            BadCredentialsException exception = assertThrows(
                BadCredentialsException.class, 
                () -> authService.login(loginRequest)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verifyNoInteractions(userRepository, tokenManager, userMapper);
        }

        @Test
        @DisplayName("Should update user online status and last seen on successful login")
        void login_UpdatesUserStatusAndLastSeen() {
            // Arrange
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tokenManager.generateToken(any(User.class))).thenReturn("accessToken");
            when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDTO);

            // Act
            authService.login(loginRequest);

            // Assert
            verify(userRepository).save(argThat(user -> 
                user.getIsOnline() && 
                user.getLastSeen() != null &&
                user.getLastSeen().isAfter(testDateTime.minusSeconds(1))
            ));
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully with valid code")
        void verifyEmail_Success() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(verificationService.VerifyCode("test@example.com", "123456")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            authService.verifyEmail(verifyRequest);

            // Assert
            verify(userRepository).findByEmail("test@example.com");
            verify(verificationService).VerifyCode("test@example.com", "123456");
            verify(userRepository).save(argThat(user -> user.getIsVerified()));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void verifyEmail_UserNotFound_ThrowsUsernameNotFoundException() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class, 
                () -> authService.verifyEmail(verifyRequest)
            );
            
            assertEquals("User not found", exception.getMessage());
            verify(userRepository).findByEmail("test@example.com");
            verifyNoInteractions(verificationService);
        }

        @Test
        @DisplayName("Should throw exception when verification code is invalid")
        void verifyEmail_InvalidCode_ThrowsIllegalArgumentException() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(verificationService.VerifyCode("test@example.com", "123456")).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, 
                () -> authService.verifyEmail(verifyRequest)
            );
            
            assertEquals("Invalid or expired verification code", exception.getMessage());
            verify(userRepository).findByEmail("test@example.com");
            verify(verificationService).VerifyCode("test@example.com", "123456");
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Forget Password Tests")
    class ForgetPasswordTests {

        @Test
        @DisplayName("Should send verification code for existing user")
        void forgetPassword_Success() {
            // Arrange
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // Act
            authService.forgetPassword("test@example.com");

            // Assert
            verify(userRepository).existsByEmail("test@example.com");
            verify(verificationService).sendVerificationCode("test@example.com");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException for non-existent user")
        void forgetPassword_UserNotFound_ThrowsUserNotFoundException() {
            // Arrange
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

            // Act & Assert
            UserNotFoundException exception = assertThrows(
                UserNotFoundException.class, 
                () -> authService.forgetPassword("test@example.com")
            );
            
            assertEquals("User not found with email: test@example.com", exception.getMessage());
            verify(userRepository).existsByEmail("test@example.com");
            verifyNoInteractions(verificationService);
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully with valid code")
        void resetPassword_Success() {
            // Arrange
            when(verificationService.VerifyCode("test@example.com", "123456")).thenReturn(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            authService.resetPassword(resetPasswordRequest);

            // Assert
            verify(verificationService).VerifyCode("test@example.com", "123456");
            verify(userRepository).findByEmail("test@example.com");
            verify(passwordEncoder).encode("newPassword123");
            verify(userRepository).save(argThat(user -> 
                user.getPassword().equals("newEncodedPassword")
            ));
        }

        @Test
        @DisplayName("Should throw exception when verification code is invalid")
        void resetPassword_InvalidCode_ThrowsIllegalArgumentException() {
            // Arrange
            when(verificationService.VerifyCode("test@example.com", "123456")).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, 
                () -> authService.resetPassword(resetPasswordRequest)
            );
            
            assertEquals("Invalid or expired code", exception.getMessage());
            verify(verificationService).VerifyCode("test@example.com", "123456");
            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void resetPassword_UserNotFound_ThrowsUsernameNotFoundException() {
            // Arrange
            when(verificationService.VerifyCode("test@example.com", "123456")).thenReturn(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class, 
                () -> authService.resetPassword(resetPasswordRequest)
            );
            
            assertEquals("User not found", exception.getMessage());
            verify(verificationService).VerifyCode("test@example.com", "123456");
            verify(userRepository).findByEmail("test@example.com");
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void logout_Success() {
            // Arrange
            testUser.setIsOnline(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            authService.logout("test@example.com");

            // Assert
            verify(userRepository).findByEmail("test@example.com");
            verify(userRepository).save(argThat(user -> 
                !user.getIsOnline() && 
                user.getLastSeen() != null &&
                user.getLastSeen().isAfter(testDateTime.minusSeconds(1))
            ));
        }

        @Test
        @DisplayName("Should handle logout for non-existent user gracefully")
        void logout_UserNotFound_HandlesGracefully() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> authService.logout("test@example.com"));
            verify(userRepository).findByEmail("test@example.com");
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully with valid refresh token")
        void refreshToken_Success() {
            // Arrange
            when(tokenManager.extractUsername("refreshToken")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(tokenManager.validateToken("refreshToken", testUser)).thenReturn(true);
            when(tokenManager.generateToken(any(User.class))).thenReturn("newAccessToken");
            when(tokenManager.generateRefreshToken(any(User.class))).thenReturn("newRefreshToken");
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDTO);

            // Act
            AuthResponse response = authService.refreshToken("refreshToken");

            // Assert
            assertNotNull(response);
            assertEquals("newAccessToken", response.getToken());
            assertEquals("newRefreshToken", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(testUserDTO, response.getUserDTO());
            
            verify(tokenManager).extractUsername("refreshToken");
            verify(userRepository).findByEmail("test@example.com");
            verify(tokenManager).validateToken("refreshToken", testUser);
            verify(tokenManager).generateToken(testUser);
            verify(tokenManager).generateRefreshToken(testUser);
            verify(userMapper).toDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void refreshToken_UserNotFound_ThrowsUsernameNotFoundException() {
            // Arrange
            when(tokenManager.extractUsername("refreshToken")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class, 
                () -> authService.refreshToken("refreshToken")
            );
            
            assertEquals("User not found", exception.getMessage());
            verify(tokenManager).extractUsername("refreshToken");
            verify(userRepository).findByEmail("test@example.com");
            verifyNoMoreInteractions(tokenManager, userRepository, userMapper);
        }

        @Test
        @DisplayName("Should throw exception when refresh token is invalid")
        void refreshToken_InvalidToken_ThrowsIllegalArgumentException() {
            // Arrange
            when(tokenManager.extractUsername("refreshToken")).thenReturn("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(tokenManager.validateToken("refreshToken", testUser)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, 
                () -> authService.refreshToken("refreshToken")
            );
            
            assertEquals("Invalid refresh token", exception.getMessage());
            verify(tokenManager).extractUsername("refreshToken");
            verify(userRepository).findByEmail("test@example.com");
            verify(tokenManager).validateToken("refreshToken", testUser);
            verifyNoMoreInteractions(tokenManager, userRepository, userMapper);
        }
    }
}