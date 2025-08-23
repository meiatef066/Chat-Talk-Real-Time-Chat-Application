package com.system.chattalk_serverside.controller.UserControllers;

import com.system.chattalk_serverside.dto.ApiResponse;
import com.system.chattalk_serverside.dto.AuthDto.*;
import com.system.chattalk_serverside.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user management APIs")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController( AuthService authService ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with email verification. User will receive a verification code via email."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Email already exists or validation failed"
            )
    })
    public ResponseEntity<Object> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called with email: {}", request.getEmail());
        var response = authService.register(request);
        ApiResponse apiResponse = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .message("User registered successfully")
                .path("/api/auth/register")
                .data(Map.of("user", response))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user credentials and returns JWT access token and refresh token. User status is updated to online."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            )
    })
    public ResponseEntity<Object> loginUser(@Valid @RequestBody LoginRequest request) {
        var response = authService.login(request);
        ApiResponse apiResponse = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Login successful")
                .path("/api/auth/login")
                .data(Map.of("user", response))
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @PutMapping("/verify-email")
    @Operation(
            summary = "Verify email address",
            description = "Marks user account as verified using the verification code sent to their email during registration."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid or expired verification code"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public ResponseEntity<Object> verifyEmail(@RequestBody VerifyRequest request) {
        log.info("Email verification endpoint called for user: {}", request.getEmail());
        authService.verifyEmail(request);
        log.info("Email verified successfully for user: {}", request.getEmail());
        ApiResponse response = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Email is verified successfully")
                .path("api/auth/verify-email")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forget-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a verification code to user's email for password reset. Code expires in 10 minutes."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset code sent successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found with provided email"
            )
    })
    public ResponseEntity<Object> forgetPassword(@RequestParam String email) {
        authService.forgetPassword(email);
        ApiResponse response = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Password reset code sent to your email")
                .path("/api/auth/forget-password")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password with verification code",
            description = "Allows user to set new password using the verification code received via email."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid or expired code"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public ResponseEntity<Object> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        ApiResponse response = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Password has been reset successfully")
                .path("/api/auth/reset-password")
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logs out the authenticated user, updates online status to offline, and records last seen timestamp.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            )
    })
    public ResponseEntity<Void> logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh JWT token",
            description = "Generates new access token using valid refresh token. Useful when access token expires."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid or expired refresh token"
            )
    })
    public ResponseEntity<Object> refreshToken(@RequestParam String token) {
        AuthResponse response = authService.refreshToken(token);
        ApiResponse apiResponse = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Token refreshed successfully")
                .path("/api/auth/refresh")
                .data(Map.of("token", response))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

}
