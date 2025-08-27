package com.system.chattalk_serverside.controller.contactController;

import com.system.chattalk_serverside.dto.ApiResponse;
import com.system.chattalk_serverside.dto.ContactDto.FriendRequestResponse;
import com.system.chattalk_serverside.dto.ContactDto.PendingFriendRequestDto;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.service.Connections.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contact & Friend Management", description = "Handles friend requests, contact list, and user relationships")
public class ContactController {
    private final ContactService contactsService;

    @Autowired
    public ContactController( ContactService contactsService ) {
        this.contactsService = contactsService;
    }

    @Operation(summary = "Send a friend request", description = "Sends a friend request to another user. The receiver will be notified and can accept or reject the request.", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "receiverEmail", description = "Email address of the user to send friend request to", required = true, example = "jane.doe@example.com"))
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Friend request sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class), examples = @ExampleObject(name = "Friend Request Response", value = "{\n" + "  \"timeStamp\": \"2024-01-15T10:30:00\",\n" + "  \"statusCode\": 200,\n" + "  \"status\": \"CREATED\",\n" + "  \"message\": \"Friend request sent successfully\",\n" + "  \"data\": {\n" + "    \"response\": {\n" + "      \"requestId\": 1,\n" + "      \"sender\": \"john.doe@example.com\",\n" + "      \"receiver\": \"jane.doe@example.com\",\n" + "      \"status\": \"PENDING\"\n" + "    }\n" + "  }\n" + "}"))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Friend request already sent or invalid data"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receiver user not found")})
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse> sendFriendRequest( @RequestParam String receiverEmail ) {
        FriendRequestResponse friendRequestResponse = contactsService.sendFriendRequest(receiverEmail);
        ApiResponse response = ApiResponse.builder().timeStamp(LocalDateTime.now()).statusCode(HttpStatus.OK.value()).status(HttpStatus.CREATED).message("Friend request sent successfully").path("/api/contacts/requests").data(Map.of("response", friendRequestResponse)).build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get pending friend requests", description = "Retrieves all pending friend requests received by the currently authenticated user.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token")})
    @GetMapping("/requests/pending")
    public ResponseEntity<ApiResponse> getPendingRequests() {
        List<PendingFriendRequestDto> pendingRequests = contactsService.getPendingRequests();
        ApiResponse response = ApiResponse.builder().timeStamp(LocalDateTime.now()).statusCode(HttpStatus.OK.value()).status(HttpStatus.OK).message("Pending friend requests fetched successfully").path("/api/contacts/requests/pending").data(Map.of("pendingRequests", pendingRequests)).build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get accepted friends", description = "Retrieves the list of all accepted friends for the currently authenticated user.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Friends list retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token")})
    @GetMapping("/friends")
    public ResponseEntity<ApiResponse> getAcceptedFriends() {
        List<UserDTO> users = contactsService.getFriends();
        ApiResponse response = ApiResponse.builder().timeStamp(LocalDateTime.now()).statusCode(HttpStatus.OK.value()).status(HttpStatus.OK).message("Friends list retrieved successfully").path("/api/contacts/friends").data(Map.of("friendList", users)).build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Accept a friend request", description = "Accepts a pending friend request from another user. Both users become friends after acceptance.", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "requestId", description = "ID of the friend request to accept", required = true, example = "1"))
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Friend request accepted successfully"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Request not found or already processed"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to accept this request")})
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse> acceptRequest( @PathVariable Long requestId ) {
        contactsService.acceptRequest(requestId);
        ApiResponse response = ApiResponse.builder().timeStamp(LocalDateTime.now()).statusCode(HttpStatus.OK.value()).status(HttpStatus.OK).message("Friend request accepted successfully").path("/api/contacts/requests/" + requestId + "/accept").build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject a friend request", description = "Rejects a pending friend request from another user. The request is marked as rejected.", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = @Parameter(name = "requestId", description = "ID of the friend request to reject", required = true, example = "1"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Friend request rejected successfully"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Request not found or already processed"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to reject this request")})
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse> rejectRequest( @PathVariable Long requestId ) {
        contactsService.rejectRequest(requestId);
        ApiResponse response = ApiResponse.builder().timeStamp(LocalDateTime.now()).statusCode(HttpStatus.OK.value()).status(HttpStatus.OK).message("Request rejected successfully").path("/api/contacts/requests/" + requestId + "/reject").build();
        return ResponseEntity.ok(response);
    }
}
