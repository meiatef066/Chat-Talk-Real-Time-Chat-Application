package com.system.chattalk_serverside.service.auth;
import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.VerificationService;
import com.system.chattalk_serverside.utils.TokenManager;
import com.system.chattalk_serverside.mapper.UserMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenManager tokenManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;
    private final UserMapper userMapper;

    @Override
    public AuthResponse register(RegisterRequest request){
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .isOnline(false)
                .lastSeen(LocalDateTime.now())
                .build();

        userRepository.save(newUser);
        verificationService.sendVerificationCode(newUser.getEmail());

        return generateAuthResponse(newUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = (User) authentication.getPrincipal();

            user.setIsOnline(true);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);

            return generateAuthResponse(user);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public void verifyEmail(VerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (verificationService.VerifyCode(request.getEmail(), request.getCode())) {
            user.setIsVerified(true);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
    }

    @Override
    public void forgetPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException(email);
        }
        verificationService.sendVerificationCode(email);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!verificationService.VerifyCode(request.getEmail(), request.getCode())) {
            throw new IllegalArgumentException("Invalid or expired code");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String email = tokenManager.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!tokenManager.validateToken(refreshToken, user)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        return generateAuthResponse(user);
    }

    // Helper
    private AuthResponse generateAuthResponse(User user) {
        return AuthResponse.builder()
                .token(tokenManager.generateToken(user))
                .refreshToken(tokenManager.generateRefreshToken(user))
                .tokenType("Bearer")
                .userDTO(userMapper.toDto(user))
                .build();
    }
}
