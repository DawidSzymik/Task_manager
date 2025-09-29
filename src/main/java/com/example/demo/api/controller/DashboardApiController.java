// src/main/java/com/example/demo/api/controller/DashboardApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.response.ActivityDto;
import com.example.demo.api.dto.response.StatsDto;
import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.service.DashboardService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardApiController(DashboardService dashboardService,
                                  UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    // GET /api/v1/dashboard/stats - Get system-wide statistics (admin only)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {

        try {
            User currentUser = getTestUser();

            // Only super admin can see system-wide stats
            if (currentUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                return createErrorResponse("Only super admin can view system statistics", HttpStatus.FORBIDDEN);
            }

            StatsDto stats = dashboardService.getSystemStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "System statistics retrieved successfully");
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve system stats: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/dashboard/user-stats - Get current user's statistics
    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {

        try {
            User currentUser = getTestUser();

            StatsDto stats = dashboardService.getUserStats(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User statistics retrieved successfully");
            response.put("data", stats);
            response.put("username", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve user stats: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/dashboard/user-stats/{userId} - Get specific user's statistics (admin)
    @GetMapping("/user-stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStatsById(@PathVariable Long userId) {

        try {
            User currentUser = getTestUser();

            // Only super admin or the user themselves can view their stats
            if (currentUser.getSystemRole() != SystemRole.SUPER_ADMIN && !currentUser.getId().equals(userId)) {
                return createErrorResponse("Access denied", HttpStatus.FORBIDDEN);
            }

            User targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            StatsDto stats = dashboardService.getUserStats(targetUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User statistics retrieved successfully");
            response.put("data", stats);
            response.put("username", targetUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve user stats: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/dashboard/recent-activity - Get recent activity
    @GetMapping("/recent-activity")
    public ResponseEntity<Map<String, Object>> getRecentActivity(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        try {
            User currentUser = getTestUser();

            // Limit max to 100
            if (limit > 100) {
                limit = 100;
            }

            List<ActivityDto> activities;

            // Super admin sees all activity, regular users see their own
            if (currentUser.getSystemRole() == SystemRole.SUPER_ADMIN) {
                activities = dashboardService.getRecentActivity(limit);
            } else {
                activities = dashboardService.getUserRecentActivity(currentUser, limit);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recent activity retrieved successfully");
            response.put("data", activities);
            response.put("count", activities.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve recent activity: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/dashboard/my-activity - Get current user's activity
    @GetMapping("/my-activity")
    public ResponseEntity<Map<String, Object>> getMyActivity(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        try {
            User currentUser = getTestUser();

            if (limit > 100) {
                limit = 100;
            }

            List<ActivityDto> activities = dashboardService.getUserRecentActivity(currentUser, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Your activity retrieved successfully");
            response.put("data", activities);
            response.put("count", activities.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve activity: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/dashboard/overview - Get complete dashboard overview
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {

        try {
            User currentUser = getTestUser();

            StatsDto userStats = dashboardService.getUserStats(currentUser);
            List<ActivityDto> recentActivity = dashboardService.getUserRecentActivity(currentUser, 10);

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("stats", userStats);
            dashboard.put("recentActivity", recentActivity);
            dashboard.put("user", Map.of(
                    "id", currentUser.getId(),
                    "username", currentUser.getUsername(),
                    "role", currentUser.getSystemRole().name()
            ));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dashboard overview retrieved successfully");
            response.put("data", dashboard);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve dashboard overview: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods
    private User getTestUser() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            throw new RuntimeException("No users found in database");
        }
        return users.get(0);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}