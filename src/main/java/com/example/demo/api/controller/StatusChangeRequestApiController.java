// ============================================================================
// PEŁNY POPRAWIONY StatusChangeRequestApiController.java
// ============================================================================
package com.example.demo.api.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/status-requests")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class StatusChangeRequestApiController {

    private final StatusChangeRequestService statusRequestService;
    private final TaskService taskService;
    private final UserService userService;
    private final ProjectMemberService projectMemberService;
    private final ProjectService projectService;

    public StatusChangeRequestApiController(
            StatusChangeRequestService statusRequestService,
            TaskService taskService,
            UserService userService,
            ProjectMemberService projectMemberService,
            ProjectService projectService) {
        this.statusRequestService = statusRequestService;
        this.taskService = taskService;
        this.userService = userService;
        this.projectMemberService = projectMemberService;
        this.projectService = projectService;
    }

    // ✅ NOWA METODA - Pobiera aktualnie zalogowanego użytkownika
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // GET /api/v1/status-requests/task/{taskId}
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> getRequestsForTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            checkTaskAccess(task, currentUser);

            List<StatusChangeRequest> requests = statusRequestService.getRequestsByTask(task);
            List<Map<String, Object>> requestDtos = requests.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", requestDtos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/status-requests
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRequest(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);
            Long taskId = ((Number) request.get("taskId")).longValue();
            String newStatus = (String) request.get("newStatus");

            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            checkTaskAccess(task, currentUser);

            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            if (userRole == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot request status changes", HttpStatus.FORBIDDEN);
            }

            // Admin can change directly
            if (userRole == ProjectRole.ADMIN) {
                task.setStatus(newStatus);
                taskService.saveTask(task);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Status changed directly (admin privilege)");
                return ResponseEntity.ok(response);
            }

            // Member must request
            StatusChangeRequest statusRequest = statusRequestService.requestStatusChange(task, newStatus, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status change request created");
            response.put("data", toDto(statusRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/status-requests/{id}/approve
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);
            StatusChangeRequest request = statusRequestService.getRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            Task task = request.getTask();
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            if (userRole != ProjectRole.ADMIN) {
                return createErrorResponse("Only admins can approve status changes", HttpStatus.FORBIDDEN);
            }

            statusRequestService.approveStatusChange(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status change approved");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/status-requests/project/{projectId}
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectRequests(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            checkProjectAccess(project, currentUser);

            ProjectRole userRole = getUserRoleInProject(project, currentUser);

            if (userRole != ProjectRole.ADMIN) {
                return createErrorResponse("Only admins can view status change requests", HttpStatus.FORBIDDEN);
            }

            List<Task> projectTasks = taskService.getTasksByProject(project);

            List<StatusChangeRequest> requests = projectTasks.stream()
                    .flatMap(task -> statusRequestService.getRequestsByTask(task).stream())
                    .collect(Collectors.toList());

            List<Map<String, Object>> requestDtos = requests.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project requests retrieved successfully");
            response.put("data", requestDtos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/status-requests/{id}/reject
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getCurrentUser(userDetails);
            String reason = body.get("reason");

            StatusChangeRequest request = statusRequestService.getRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            Task task = request.getTask();
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            if (userRole != ProjectRole.ADMIN) {
                return createErrorResponse("Only admins can reject status changes", HttpStatus.FORBIDDEN);
            }

            statusRequestService.rejectStatusChange(id, currentUser, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status change rejected");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private void checkTaskAccess(Task task, User user) {
        ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), user)
                .orElse(null);
        if (membership == null && user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Access denied");
        }
    }

    private void checkProjectAccess(Project project, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return;
        }

        boolean isMember = projectMemberService.getProjectMember(project, user).isPresent();
        if (!isMember) {
            throw new IllegalArgumentException("Access denied: User is not a member of this project");
        }
    }

    private ProjectRole getUserRoleInProject(Project project, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return ProjectRole.ADMIN;
        }

        return projectMemberService.getProjectMember(project, user)
                .map(ProjectMember::getRole)
                .orElse(ProjectRole.VIEWER);
    }

    private Map<String, Object> toDto(StatusChangeRequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", request.getId());
        dto.put("currentStatus", request.getCurrentStatus());
        dto.put("requestedStatus", request.getRequestedStatus());
        dto.put("status", request.getStatus().toString());
        dto.put("createdAt", request.getCreatedAt().toString());

        Map<String, Object> requestedBy = new HashMap<>();
        requestedBy.put("id", request.getRequestedBy().getId());
        requestedBy.put("username", request.getRequestedBy().getUsername());
        dto.put("requestedBy", requestedBy);

        Map<String, Object> task = new HashMap<>();
        task.put("id", request.getTask().getId());
        task.put("title", request.getTask().getTitle());
        dto.put("task", task);

        if (request.getReviewedBy() != null) {
            Map<String, Object> reviewedBy = new HashMap<>();
            reviewedBy.put("id", request.getReviewedBy().getId());
            reviewedBy.put("username", request.getReviewedBy().getUsername());
            dto.put("reviewedBy", reviewedBy);
        }

        if (request.getReviewedAt() != null) {
            dto.put("reviewedAt", request.getReviewedAt().toString());
        }

        if (request.getRejectionReason() != null) {
            dto.put("rejectionReason", request.getRejectionReason());
        }

        return dto;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}