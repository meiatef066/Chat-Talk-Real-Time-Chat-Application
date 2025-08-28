package com.system.chattalk_serverside.controller;

import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.service.Notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "Handles user notifications including retrieval and status updates")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController( NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user")
    public
    ResponseEntity<List<NotificationDTO>> getNotifications() {
        List<NotificationDTO> notifications = notificationService.getNotification();
        return ResponseEntity.ok(notifications);
    }

    @DeleteMapping
    @Operation(summary = "Delete all notifications for the authenticated user")
    public ResponseEntity<Void> deleteAllNotifications() {
        notificationService.deleteAllNotification();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a specific notification by ID")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        NotificationDTO updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

}
