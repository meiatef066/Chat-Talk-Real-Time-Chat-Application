package com.system.chattalk_serverside.controller.UserControllers;

import com.system.chattalk_serverside.dto.ApiResponse;
import com.system.chattalk_serverside.dto.AuthDto.UpdateProfileRequest;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("api/profile/me")
@Slf4j
@Tag(name = "User Profile", description = "User profile management and updates")
public class ProfileController {
    private final ProfileService profileService;
    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the complete profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Profile Response",
                                    value = "{\n" +
                                            "  \"timeStamp\": \"2024-01-15T10:30:00\",\n" +
                                            "  \"statusCode\": 200,\n" +
                                            "  \"status\": \"OK\",\n" +
                                            "  \"message\": \"Fetch User Data\",\n" +
                                            "  \"data\": {\n" +
                                            "    \"user\": {\n" +
                                            "      \"id\": 1,\n" +
                                            "      \"email\": \"john.doe@example.com\",\n" +
                                            "      \"firstName\": \"John\",\n" +
                                            "      \"lastName\": \"Doe\",\n" +
                                            "      \"profilePictureUrl\": \"https://example.com/profile.jpg\",\n" +
                                            "      \"phoneNumber\": \"+1234567890\",\n" +
                                            "      \"bio\": \"Software Developer\",\n" +
                                            "      \"gender\": \"MALE\",\n" +
                                            "      \"dateOfBirth\": \"1990-01-01\"\n" +
                                            "    }\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User profile not found"
            )
    })
    public ResponseEntity<ApiResponse> GetUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserDTO userDTO = profileService.GetProfile(email);
        ApiResponse apiResponse = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Fetch User Data")
                .path("/api/profile/me")
                .data(Map.of("user", userDTO))
                .build();

        return ResponseEntity.ok(apiResponse);
    }
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update user profile",
            description = "Updates the profile information of the currently authenticated user. Only provided fields will be updated.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Profile update details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateProfileRequest.class),
                            examples = @ExampleObject(
                                    name = "Profile Update",
                                    value = "{\n" +
                                            "  \"firstName\": \"John\",\n" +
                                            "  \"lastName\": \"Smith\",\n" +
                                            "  \"bio\": \"Senior Software Developer\",\n" +
                                            "  \"gender\": \"MALE\",\n" +
                                            "  \"dateOfBirth\": \"1990-01-01\",\n" +
                                            "  \"phoneNumber\": \"+1234567890\"\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid data format or validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = "{\n" +
                                            "  \"timeStamp\": \"2024-01-15T10:30:00\",\n" +
                                            "  \"statusCode\": 400,\n" +
                                            "  \"status\": \"BAD_REQUEST\",\n" +
                                            "  \"message\": \"Failed to update profile: Invalid date format. Please use yyyy-MM-dd format.\",\n" +
                                            "  \"path\": \"/api/profile/me\"\n" +
                                            "}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            )
    })
    public ResponseEntity<ApiResponse> UpdateUserProfile(@RequestBody UpdateProfileRequest request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Received profile update request for user: {}", email);
            log.info("Request fields: firstName={}, lastName={}, bio={}, gender={}, dateOfBirth={}, phoneNumber={}",
                    request.getFirstName(), request.getLastName(), request.getBio(), request.getGender(),
                    request.getDateOfBirth(), request.getPhoneNumber());

            UserDTO userDTO = profileService.UpdateProfile(request);
            ApiResponse apiResponse = ApiResponse.builder()
                    .timeStamp(LocalDateTime.now())
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .message("Update User Profile successfully")
                    .path("/api/profile/me")
                    .data(Map.of("user", userDTO))
                    .build();

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            ApiResponse apiResponse = ApiResponse.builder()
                    .timeStamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Failed to update profile: " + e.getMessage())
                    .path("/api/profile/me")
                    .build();
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }
    public ResponseEntity<ApiResponse> UpdateProfilePicture(@RequestParam("profilePicture") MultipartFile profilePicture) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Received profile picture update request for user: {}", email);
            log.info("Picture file: name={}, size={}, content-type={}",
                    profilePicture.getOriginalFilename(), profilePicture.getSize(), profilePicture.getContentType());

            UserDTO userDTO = profileService.UpdateProfilePicture(profilePicture);
            ApiResponse apiResponse = ApiResponse.builder()
                    .timeStamp(LocalDateTime.now())
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .message("Profile picture updated successfully")
                    .path("/api/profile/me/picture")
                    .data(Map.of("user", userDTO))
                    .build();

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error updating profile picture: ", e);
            ApiResponse apiResponse = ApiResponse.builder()
                    .timeStamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Failed to update profile picture: " + e.getMessage())
                    .path("/api/profile/me/picture")
                    .build();
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @PutMapping("/change-password")
    @Operation(
            summary = "Change user password",
            description = "Changes the password of the currently authenticated user. Requires current password verification.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            parameters = {
                    @Parameter(
                            name = "oldPassword",
                            description = "Current password for verification",
                            required = true,
                            example = "currentPassword123"
                    ),
                    @Parameter(
                            name = "newPassword",
                            description = "New password (minimum 8 characters)",
                            required = true,
                            example = "newSecurePassword123"
                    )
            }
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Current password is incorrect or new password is invalid"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            )
    })
    public ResponseEntity<ApiResponse> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        profileService.changePassword(email, oldPassword, newPassword);

        ApiResponse apiResponse = ApiResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Password changed successfully")
                .path("/api/profile/me/change-password")
                .build();

        return ResponseEntity.ok(apiResponse);
    }

}
