package com.system.chattalk_serverside.service;

import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.utils.TokenManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final TokenManager tokenManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService( UserRepository userRepository, TokenManager tokenManager, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager ) {
        this.userRepository = userRepository;
        this.tokenManager = tokenManager;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }
//    private final VerificationService verificationService;


}
