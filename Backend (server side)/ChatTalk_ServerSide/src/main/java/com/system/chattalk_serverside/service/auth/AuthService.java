package com.system.chattalk_serverside.service.auth;


import com.system.chattalk_serverside.dto.AuthDto.*;

public interface AuthService {
    AuthResponse register( RegisterRequest request);
    AuthResponse login( LoginRequest request);
    void verifyEmail( VerifyRequest request);
    void forgetPassword(String email);
    void resetPassword( ResetPasswordRequest request);
    void logout(String email);
    AuthResponse refreshToken(String refreshToken);
}
