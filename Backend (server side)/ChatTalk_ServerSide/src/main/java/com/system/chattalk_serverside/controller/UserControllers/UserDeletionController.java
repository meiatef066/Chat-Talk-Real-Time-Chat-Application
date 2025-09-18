package com.system.chattalk_serverside.controller.UserControllers;

import com.system.chattalk_serverside.service.User.UserDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Deletion", description = "APIs for user account deletion")
@SecurityRequirement(name = "Bearer Authentication")
public class UserDeletionController {
    
    private final UserDeletionService userDeletionService;

    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete user account", 
        description = "Soft delete user account - marks user as deleted but keeps data for referential integrity",
        parameters = @Parameter(name = "userId", description = "ID of the user to delete", required = true, example = "164")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID or user cannot be deleted"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<String> softDeleteUser(@PathVariable Long userId) {
        try {
            // Check if user can be safely deleted
            if (!userDeletionService.canDeleteUser(userId)) {
                return ResponseEntity.badRequest()
                    .body("User cannot be deleted - they are the only admin in one or more group chats");
            }
            
            userDeletionService.softDeleteUser(userId);
            log.info("User {} successfully soft deleted", userId);
            return ResponseEntity.ok("User account successfully deleted");
            
        } catch (RuntimeException e) {
            log.error("Error deleting user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/hard")
    @Operation(
        summary = "Hard delete user account", 
        description = "Permanently delete user account and all related data. WARNING: This action cannot be undone!",
        parameters = @Parameter(name = "userId", description = "ID of the user to permanently delete", required = true, example = "164")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User permanently deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID or user cannot be deleted"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<String> hardDeleteUser(@PathVariable Long userId) {
        try {
            // Check if user can be safely deleted
            if (!userDeletionService.canDeleteUser(userId)) {
                return ResponseEntity.badRequest()
                    .body("User cannot be deleted - they are the only admin in one or more group chats");
            }
            
            userDeletionService.hardDeleteUser(userId);
            log.info("User {} successfully hard deleted", userId);
            return ResponseEntity.ok("User account permanently deleted");
            
        } catch (RuntimeException e) {
            log.error("Error hard deleting user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting user: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/can-delete")
    @Operation(
        summary = "Check if user can be deleted", 
        description = "Check if a user account can be safely deleted",
        parameters = @Parameter(name = "userId", description = "ID of the user to check", required = true, example = "164")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Boolean> canDeleteUser(@PathVariable Long userId) {
        try {
            boolean canDelete = userDeletionService.canDeleteUser(userId);
            return ResponseEntity.ok(canDelete);
        } catch (RuntimeException e) {
            log.error("Error checking if user {} can be deleted: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(false);
        }
    }

    @DeleteMapping("/me")
    @Operation(
        summary = "Delete current user account", 
        description = "Soft delete the currently authenticated user's account"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully deleted"),
        @ApiResponse(responseCode = "400", description = "User cannot be deleted"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<String> deleteCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof com.system.chattalk_serverside.model.User)) {
                return ResponseEntity.badRequest().body("User not authenticated");
            }
            
            com.system.chattalk_serverside.model.User currentUser = 
                (com.system.chattalk_serverside.model.User) auth.getPrincipal();
            
            Long userId = currentUser.getId();
            
            // Check if user can be safely deleted
            if (!userDeletionService.canDeleteUser(userId)) {
                return ResponseEntity.badRequest()
                    .body("Your account cannot be deleted - you are the only admin in one or more group chats");
            }
            
            userDeletionService.softDeleteUser(userId);
            log.info("Current user {} successfully soft deleted", userId);
            return ResponseEntity.ok("Your account has been successfully deleted");
            
        } catch (RuntimeException e) {
            log.error("Error deleting current user: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting account: " + e.getMessage());
        }
    }
}
