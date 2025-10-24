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

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

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

    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long taskId,
            @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);

            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);
            if (userRole == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot add comments", HttpStatus.FORBIDDEN);
            }

            Comment comment = commentMapper.toEntity(request, currentUser);
            comment.setTask(task);

            Comment savedComment = commentService.saveComment(comment);
            CommentDto commentDto = commentMapper.toDtoWithPermissions(savedComment, currentUser, userRole);

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

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

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

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            Comment comment = commentService.getCommentById(id)
                    .orElseThrow(() -> new RuntimeException("Comment with ID " + id + " not found"));

            checkTaskAccess(comment.getTask(), currentUser);

            ProjectRole userRole = getUserRoleInProject(comment.getTask().getProject(), currentUser);
            boolean isAuthor = comment.getAuthor().equals(currentUser);
            boolean isAdmin = userRole == ProjectRole.ADMIN;

            if (!isAuthor && !isAdmin) {
                return createErrorResponse("Only comment author or project admin can edit this comment", HttpStatus.FORBIDDEN);
            }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            Comment comment = commentService.getCommentById(id)
                    .orElseThrow(() -> new RuntimeException("Comment with ID " + id + " not found"));

            checkTaskAccess(comment.getTask(), currentUser);

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

    private User getUserFromDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }
        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void checkTaskAccess(Task task, User user) {
        ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), user)
                .orElse(null);

        if (membership == null && user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Access denied to task with ID " + task.getId());
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

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}