package com.system.chattalk_serverside.controller.UserControllers;

import com.system.chattalk_serverside.dto.ApiResponse;
import com.system.chattalk_serverside.service.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Search", description = "Search and discover users to send friend requests")
public class UserSearchController {
    private final UserSearchService userService;

    @Autowired
    public UserSearchController( UserSearchService userService ) {
        this.userService = userService;
    }

    @Operation(summary = "Search for users", description = "Searches for users based on query string. Results include relationship status (ACCEPTED, BLOCKED, PENDING, NONE) with the current user. Useful for finding new friends to connect with.", security = @SecurityRequirement(name = "Bearer Authentication"), parameters = {@Parameter(name = "query", description = "Search query to find users by name, email, or phone number", required = true, example = "john"), @Parameter(name = "currentUserId", description = "ID of the current authenticated user", required = true, example = "1"), @Parameter(name = "page", description = "Page number for pagination (0-based, default: 0)", required = false, example = "0"), @Parameter(name = "size", description = "Number of results per page (default: 10)", required = false, example = "10")})
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.system.chattalk_serverside.dto.ApiResponse.class), examples = @ExampleObject(name = "Search Results", value = "{\n" + "  \"status\": \"OK\",\n" + "  \"statusCode\": 200,\n" + "  \"message\": \"Search results\",\n" + "  \"data\": {\n" + "    \"results\": [\n" + "      {\n" + "        \"id\": 2,\n" + "        \"username\": \"jane_doe\",\n" + "        \"email\": \"jane.doe@example.com\",\n" + "        \"firstName\": \"Jane\",\n" + "        \"lastName\": \"Doe\",\n" + "        \"relationshipStatus\": \"NONE\"\n" + "      }\n" + "    ],\n" + "    \"page\": 0,\n" + "    \"size\": 10,\n" + "    \"totalElements\": 1,\n" + "    \"totalPages\": 1\n" + "  }\n" + "}"))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Invalid search parameters"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token")})
    @GetMapping("/search")
    public ResponseEntity<Object> searchUsers( @RequestParam String query, @RequestParam Long currentUserId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size ) {
        Page<?> users = userService.searchForNewFriends(query, currentUserId, page, size);

        ApiResponse<Object> response = ApiResponse.builder().status(HttpStatus.OK).statusCode(HttpStatus.OK.value()).message("Search results").data(Map.of("results", users.getContent(), "page", users.getNumber(), "size", users.getSize(), "totalElements", users.getTotalElements(), "totalPages", users.getTotalPages())).build();

        return ResponseEntity.ok(response);
    }

}
