// src/main/java/com/example/demo/api/controller/NotificationApiController.java
package com.example.demo.api.controller;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationApiController(NotificationService notificationService,
                                     UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // GET /api/v1/notifications - Get all notifications for current user
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNotifications(
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly) {

        try {
            User currentUser = getTestUser();

            List<Notification> notifications;
            if (unreadOnly) {
                notifications = notificationService.getUnreadNotifications(currentUser);
            } else {
                notifications = notificationService.getUserNotifications(currentUser);
            }

            long unreadCount = notificationService.getUnreadCount(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notifications retrieved successfully");
            response.put("data", notifications);
            response.put("unreadCount", unreadCount);
            response.put("totalCount", notifications.size());
            response.put("testUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve notifications: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/notifications/{id} - Get specific notification
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNotification(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            Notification notification = notificationService.getUserNotifications(currentUser)
                    .stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Notification with ID " + id + " not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification retrieved successfully");
            response.put("data", notification);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve notification: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/notifications/unread-count - Get count of unread notifications
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {

        try {
            User currentUser = getTestUser();
            long unreadCount = notificationService.getUnreadCount(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve unread count: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/notifications/{id}/mark-read - Mark notification as read
    @PutMapping("/{id}/mark-read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            // Verify notification belongs to current user
            Notification notification = notificationService.getUserNotifications(currentUser)
                    .stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Notification with ID " + id + " not found"));

            notificationService.markAsRead(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification marked as read successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to mark notification as read: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/notifications/mark-all-read - Mark all notifications as read
    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {

        try {
            User currentUser = getTestUser();
            notificationService.markAllAsRead(currentUser);

            long remainingUnread = notificationService.getUnreadCount(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read successfully");
            response.put("remainingUnread", remainingUnread);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to mark all notifications as read: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/notifications/{id} - Delete notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            // Verify notification belongs to current user before deleting
            Notification notification = notificationService.getUserNotifications(currentUser)
                    .stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Notification with ID " + id + " not found"));

            notificationService.deleteNotification(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete notification: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/notifications - Delete all notifications for current user
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllNotifications(
            @RequestParam(value = "readOnly", defaultValue = "false") boolean readOnly) {

        try {
            User currentUser = getTestUser();

            List<Notification> notificationsToDelete;
            if (readOnly) {
                // Delete only read notifications
                notificationsToDelete = notificationService.getUserNotifications(currentUser)
                        .stream()
                        .filter(Notification::isRead)
                        .toList();
            } else {
                // Delete all notifications
                notificationsToDelete = notificationService.getUserNotifications(currentUser);
            }

            int deletedCount = 0;
            for (Notification notification : notificationsToDelete) {
                notificationService.deleteNotification(notification.getId());
                deletedCount++;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletedCount + " notification(s) deleted successfully");
            response.put("deletedCount", deletedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete notifications: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods
    private User getTestUser() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            throw new RuntimeException("No users found in database");
        }
        return users.get(0); // Uses first user for testing
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}