// src/main/java/com/example/demo/api/controller/CommentApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.CreateCommentRequest;
import com.example.demo.api.dto.request.UpdateCommentRequest;
import com.example.demo.api.dto.response.CommentDto;
import com.example.demo.api.mapper.CommentMapper;
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

@RestController
@RequestMapping("/api/v1/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class CommentApiController {

    private final CommentService commentService;
    private final TaskService taskService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;
    private final CommentMapper commentMapper;

    public CommentApiController(CommentService commentService,
                                TaskService taskService,
                                ProjectMemberService projectMemberService,
                                UserService userService,
                                CommentMapper commentMapper) {
        this.commentService = commentService;
        this.taskService = taskService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
        this.commentMapper = commentMapper;
    }

    // GET /api/v1/tasks/{taskId}/comments - Get comments for specific task
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            // Get task and check access
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);

            // Get user role in project
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            // Get comments
            List<Comment> comments = commentService.getCommentsByTask(task);
            List<CommentDto> commentDtos = commentMapper.toDtoWithPermissions(comments, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comments retrieved successfully");
            response.put("data", commentDtos);
            response.put("taskId", taskId);
            response.put("taskTitle", task.getTitle());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve comments: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/tasks/{taskId}/comments - Create new comment
    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long taskId,
            @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // ✅ TUTAJ JEST NAPRAWA - używamy prawdziwego zalogowanego użytkownika!
            User currentUser = getCurrentUser(userDetails);

            System.out.println("🔍 Creating comment by user: " + currentUser.getUsername());

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            // Get task and check access
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);

            // Check if user can comment (not viewer)
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);
            if (userRole == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot add comments", HttpStatus.FORBIDDEN);
            }

            // Create comment with actual logged-in user as author
            Comment comment = commentMapper.toEntity(request, currentUser);
            comment.setTask(task);

            Comment savedComment = commentService.saveComment(comment);
            CommentDto commentDto = commentMapper.toDtoWithPermissions(savedComment, currentUser, userRole);

            System.out.println("✅ Comment created successfully by: " + savedComment.getAuthor().getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment created successfully");
            response.put("data", commentDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to create comment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/comments/{id} - Get specific comment
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Comment comment = commentService.getCommentById(id)
                    .orElseThrow(() -> new RuntimeException("Comment with ID " + id + " not found"));

            checkTaskAccess(comment.getTask(), currentUser);

            ProjectRole userRole = getUserRoleInProject(comment.getTask().getProject(), currentUser);
            CommentDto commentDto = commentMapper.toDtoWithPermissions(comment, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment retrieved successfully");
            response.put("data", commentDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve comment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/comments/{id} - Update comment
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            Comment comment = commentService.getCommentById(id)
                    .orElseThrow(() -> new RuntimeException("Comment with ID " + id + " not found"));

            checkTaskAccess(comment.getTask(), currentUser);

            // Check if user can edit (author or admin)
            ProjectRole userRole = getUserRoleInProject(comment.getTask().getProject(), currentUser);
            boolean isAuthor = comment.getAuthor().equals(currentUser);
            boolean isAdmin = userRole == ProjectRole.ADMIN;

            if (!isAuthor && !isAdmin) {
                return createErrorResponse("Only comment author or project admin can edit this comment", HttpStatus.FORBIDDEN);
            }

            // Update comment
            commentMapper.updateEntity(comment, request);
            Comment updatedComment = commentService.saveComment(comment);
            CommentDto commentDto = commentMapper.toDtoWithPermissions(updatedComment, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment updated successfully");
            response.put("data", commentDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to update comment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/comments/{id} - Delete comment
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Comment comment = commentService.getCommentById(id)
                    .orElseThrow(() -> new RuntimeException("Comment with ID " + id + " not found"));

            checkTaskAccess(comment.getTask(), currentUser);

            // Check if user can delete (author or admin)
            ProjectRole userRole = getUserRoleInProject(comment.getTask().getProject(), currentUser);
            boolean isAuthor = comment.getAuthor().equals(currentUser);
            boolean isAdmin = userRole == ProjectRole.ADMIN;

            if (!isAuthor && !isAdmin) {
                return createErrorResponse("Only comment author or project admin can delete this comment", HttpStatus.FORBIDDEN);
            }

            commentService.deleteComment(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete comment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods

    /**
     * ✅ NOWA METODA - Pobiera aktualnie zalogowanego użytkownika
     * Zamiast getTestUser() która zwracała zawsze pierwszego użytkownika (admina)
     */
    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }

        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    private void checkTaskAccess(Task task, User user) {
        // Check if user has access to the project containing this task
        ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), user)
                .orElse(null);

        if (membership == null && user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Access denied to task with ID " + task.getId());
        }
    }

    private ProjectRole getUserRoleInProject(Project project, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return ProjectRole.ADMIN; // Super admin has admin privileges
        }

        return projectMemberService.getProjectMember(project, user)
                .map(ProjectMember::getRole)
                .orElse(ProjectRole.VIEWER); // Default to viewer if not found
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}