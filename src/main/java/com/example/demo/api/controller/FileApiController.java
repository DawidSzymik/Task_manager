// src/main/java/com/example/demo/api/controller/FileApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.response.FileDto;
import com.example.demo.api.mapper.FileMapper;
import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class FileApiController {

    private final FileService fileService;
    private final TaskService taskService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;
    private final FileMapper fileMapper;

    public FileApiController(FileService fileService,
                             TaskService taskService,
                             ProjectMemberService projectMemberService,
                             UserService userService,
                             FileMapper fileMapper) {
        this.fileService = fileService;
        this.taskService = taskService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
        this.fileMapper = fileMapper;
    }

    // GET /api/v1/files/tasks/{taskId} - Get files for specific task
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskFiles(@PathVariable Long taskId) {

        try {
            User currentUser = getTestUser();

            // Get task and check access
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);

            // Get user role in project
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            // Get files
            List<UploadedFile> files = fileService.getFilesByTask(task);
            List<FileDto> fileDtos = fileMapper.toDtoWithPermissions(files, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Files retrieved successfully");
            response.put("data", fileDtos);
            response.put("taskId", taskId);
            response.put("taskTitle", task.getTitle());
            response.put("totalFiles", files.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve files: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/files/tasks/{taskId} - Upload file to task
    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {

        try {
            User currentUser = getTestUser();

            // Validate file
            if (file.isEmpty()) {
                return createErrorResponse("File is empty", HttpStatus.BAD_REQUEST);
            }

            // Check file size (max 10MB)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return createErrorResponse("File size exceeds maximum limit of 10MB", HttpStatus.BAD_REQUEST);
            }

            // Get task and check access
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);

            // Check if user can upload files (not viewer)
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);
            if (userRole == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot upload files", HttpStatus.FORBIDDEN);
            }

            // Upload file
            UploadedFile uploadedFile = fileService.storeFile(task, file, currentUser);
            FileDto fileDto = fileMapper.toDtoWithPermissions(uploadedFile, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("data", fileDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/files/{id} - Get file metadata
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            ProjectRole userRole = getUserRoleInProject(file.getTask().getProject(), currentUser);
            FileDto fileDto = fileMapper.toDtoWithPermissions(file, currentUser, userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File retrieved successfully");
            response.put("data", fileDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/files/{id}/download - Download file
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            if (file.getData() == null) {
                throw new RuntimeException("File data not found");
            }

            ByteArrayResource resource = new ByteArrayResource(file.getData());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .contentLength(file.getData().length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DELETE /api/v1/files/{id} - Delete file
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            // Check if user can delete (uploader or admin)
            ProjectRole userRole = getUserRoleInProject(file.getTask().getProject(), currentUser);
            boolean isUploader = file.getUploadedBy() != null && file.getUploadedBy().equals(currentUser);
            boolean isAdmin = userRole == ProjectRole.ADMIN;

            if (!isUploader && !isAdmin) {
                return createErrorResponse("Only file uploader or project admin can delete this file", HttpStatus.FORBIDDEN);
            }

            fileService.deleteFile(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/files/user/{userId} - Get files uploaded by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserFiles(@PathVariable Long userId) {

        try {
            User currentUser = getTestUser();

            User targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Only allow viewing own files or super admin
            if (!currentUser.equals(targetUser) && currentUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                return createErrorResponse("Access denied", HttpStatus.FORBIDDEN);
            }

            List<UploadedFile> files = fileService.getFilesByUser(targetUser);
            List<FileDto> fileDtos = fileMapper.toDto(files);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User files retrieved successfully");
            response.put("data", fileDtos);
            response.put("userId", userId);
            response.put("totalFiles", files.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve user files: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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