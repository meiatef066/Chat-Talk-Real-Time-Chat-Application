package com.system.chattalk_serverside.service;

import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.enums.UserStatus;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.utils.TokenManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final TokenManager tokenManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;

    @Autowired
    public AuthService( UserRepository userRepository, TokenManager tokenManager, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, VerificationService verificationService ) {
        this.userRepository = userRepository;
        this.tokenManager = tokenManager;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.verificationService = verificationService;
    }

    public AuthResponse register( RegisterRequest request){
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User newUser = User.builder().email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).firstName(request.getFirstName()).lastName(request.getLastName())
                .status(UserStatus.ACTIVE).isVerified(false).isOnline(false).lastSeen(LocalDateTime.now()).build();

        userRepository.save(newUser);
        log.info("User registered successfully: {}", newUser.getEmail());

        // send verification email
        verificationService.sendVerificationCode(newUser.getEmail());
        log.info("email send to user : {}", newUser.getEmail());

        return generateAuthResponse(newUser);
    }

    public AuthResponse login( LoginRequest request ) {
        log.info("Processing login for user: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = (User) authentication.getPrincipal();

            user.setIsOnline(true);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);

            log.info("User logged in successfully: {}", user.getEmail());
            return generateAuthResponse(user);
        } catch (BadCredentialsException e) {
            log.warn("Login failed: {}", request.getEmail());
            throw new BadCredentialsException("Bad credentials");
        }
    }

    //Helper
    private AuthResponse generateAuthResponse( User user ) {
        String accessToken = tokenManager.generateToken(user);
        String refreshToken = tokenManager.generateRefreshToken(user);

        return AuthResponse.builder().token(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .userDTO(UserDTO.builder().id(user.getId()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName()).profilePictureUrl(user.getProfilePictureUrl()).isOnline(user.getIsOnline()).isVerified(user.getIsVerified()).build()).build();
    }

    public void forgetPassword( String email ) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException(email);
        }

        verificationService.sendVerificationCode(email);

    }

    public void resetPassword( ResetPasswordRequest request) {
        boolean isVerified = verificationService.VerifyCode(request.getEmail(), request.getCode());

        if (!isVerified) {
            throw new IllegalArgumentException("Invalid or expired code");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password updated for user: {}", user.getEmail());
    }


}
