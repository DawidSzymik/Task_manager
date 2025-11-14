// src/main/java/com/example/demo/api/controller/FileApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.response.FileDto;
import com.example.demo.api.mapper.FileMapper;
import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class FileApiController {

    @Autowired
    private AzureBlobService blobService;

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

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskFiles(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + taskId + " not found"));

            checkTaskAccess(task, currentUser);
            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

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

    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            checkTaskAccess(task, currentUser);

            System.out.println("📤 Uploading file: " + file.getOriginalFilename());
            System.out.println("📦 Size: " + file.getSize() + " bytes");

            Long projectId = task.getProject().getId();
            String blobUrl = blobService.uploadFile(file, projectId);

            UploadedFile uploadedFile = fileService.saveFile(task, file, currentUser, blobUrl);

            System.out.println("✅ File metadata saved to database");

            // ✅ ZMIENIONE - zwracamy pełny obiekt w "data"
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");

            // ✅ Pełny obiekt pliku w "data"
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("id", uploadedFile.getId());
            fileData.put("originalName", uploadedFile.getOriginalName());
            fileData.put("contentType", uploadedFile.getContentType());
            fileData.put("fileSize", uploadedFile.getFileSize());
            fileData.put("blobUrl", blobUrl);
            fileData.put("uploadedAt", uploadedFile.getUploadedAt());
            fileData.put("uploadedBy", Map.of(
                    "id", currentUser.getId(),
                    "username", currentUser.getUsername(),
                    "fullName", currentUser.getFullName()
            ));

            response.put("data", fileData); // ✅ KLUCZOWA ZMIANA!

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            System.err.println("❌ Upload error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

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

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            byte[] fileData;

            // ✅ NOWA LOGIKA - sprawdź czy plik jest w Blob Storage
            if (file.getBlobUrl() != null && !file.getBlobUrl().isEmpty()) {
                System.out.println("📥 Downloading from Azure Blob Storage");
                fileData = blobService.downloadFile(file.getBlobUrl());
            } else {
                // Fallback dla starych plików (w bazie)
                System.out.println("📥 Downloading from database (old file)");
                fileData = file.getData();
            }

            if (fileData == null || fileData.length == 0) {
                throw new RuntimeException("File data not found");
            }

            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .contentLength(fileData.length)
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

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

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserFiles(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            User targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

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
// src/main/java/com/example/demo/api/controller/FileApiController.java
// DODAJ TEN ENDPOINT DO ISTNIEJĄCEGO FileApiController

    /**
     * Endpoint do podglądu pliku w przeglądarce (inline)
     * GET /api/v1/files/{id}/preview
     */
    /**
     * Endpoint do podglądu pliku w przeglądarce (inline)
     * GET /api/v1/files/{id}/preview
     */
    /**
     * Endpoint do podglądu pliku w przeglądarce (inline)
     * GET /api/v1/files/{id}/preview
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<ByteArrayResource> previewFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            byte[] fileData;

            // ✅ Sprawdź czy plik jest w Blob Storage
            if (file.getBlobUrl() != null && !file.getBlobUrl().isEmpty()) {
                System.out.println("📥 Preview from Azure Blob Storage");
                fileData = blobService.downloadFile(file.getBlobUrl());
            } else {
                // Fallback dla starych plików (w bazie)
                System.out.println("📥 Preview from database (old file)");
                fileData = file.getData();
            }

            if (fileData == null || fileData.length == 0) {
                throw new RuntimeException("File data not found");
            }

            ByteArrayResource resource = new ByteArrayResource(fileData);

            // Content-Disposition = "inline" → otwiera w przeglądarce zamiast pobierać
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .contentLength(fileData.length)
                    .cacheControl(CacheControl.maxAge(3600, java.util.concurrent.TimeUnit.SECONDS))
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

    /**
     * Sprawdź czy plik można wyświetlić w podglądzie
     * GET /api/v1/files/{id}/can-preview
     */
    @GetMapping("/{id}/can-preview")
    public ResponseEntity<Map<String, Object>> canPreview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getUserFromDetails(userDetails);

            UploadedFile file = fileService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File with ID " + id + " not found"));

            checkTaskAccess(file.getTask(), currentUser);

            boolean canPreview = isPreviewable(file.getContentType());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "canPreview", canPreview,
                    "contentType", file.getContentType()
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to check preview capability: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method
    private boolean isPreviewable(String contentType) {
        if (contentType == null) return false;

        return contentType.equals("application/pdf") ||
                contentType.startsWith("image/") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("application/vnd.ms-excel");
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