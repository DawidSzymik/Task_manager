// src/main/java/com/example/demo/api/controller/TaskApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.CreateTaskRequest;
import com.example.demo.api.dto.request.UpdateTaskRequest;
import com.example.demo.api.dto.response.TaskDto;
import com.example.demo.api.mapper.TaskMapper;
import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final TaskMapper taskMapper;

    public TaskApiController(TaskService taskService,
                             ProjectService projectService,
                             ProjectMemberService projectMemberService,
                             UserService userService,
                             NotificationService notificationService,
                             TaskMapper taskMapper) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.taskMapper = taskMapper;
    }

    // ✅ POPRAWIONA METODA - używa prawdziwego zalogowanego użytkownika
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    private void checkProjectAccess(Project project, User user) {
        var membership = projectMemberService.getProjectMember(project, user);
        if (membership.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
    }

    private void checkTaskAccess(Task task, User user) {
        checkProjectAccess(task.getProject(), user);
    }

    private ProjectRole getUserRoleInProject(Project project, User user) {
        return projectMemberService.getProjectMember(project, user)
                .map(ProjectMember::getRole)
                .orElse(null);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }

    // ============================================================================
    // GET ENDPOINTS
    // ============================================================================

    // GET /api/v1/tasks/{id} - Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            checkTaskAccess(task, currentUser);

            TaskDto taskDto = taskMapper.toDtoWithStats(task);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task retrieved successfully");
            response.put("data", taskDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/tasks - Get all tasks (with optional filters)
    // ✅ POPRAWIONA LOGIKA: SUPER_ADMIN widzi WSZYSTKIE zadania z systemu
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assignedToMe", required = false) Boolean assignedToMe,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            List<Task> tasks;

            // ✅ NOWA LOGIKA DLA SUPER_ADMIN
            // Jeśli użytkownik jest SUPER_ADMIN i assignedToMe=false, zwróć WSZYSTKIE zadania z systemu
            if (currentUser.getSystemRole() == SystemRole.SUPER_ADMIN && Boolean.FALSE.equals(assignedToMe)) {
                System.out.println("🔥 SUPER_ADMIN requesting ALL system tasks");

                if (projectId != null) {
                    // SUPER_ADMIN + projectId: wszystkie zadania z konkretnego projektu
                    Project project = projectService.getProjectById(projectId)
                            .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));
                    tasks = taskService.getTasksByProject(project);
                    System.out.println("✅ Found " + tasks.size() + " tasks in project " + projectId);
                } else {
                    // SUPER_ADMIN bez projectId: WSZYSTKIE zadania z CAŁEGO systemu
                    tasks = taskService.getAllTasks();
                    System.out.println("✅ Found " + tasks.size() + " TOTAL tasks in system");
                }
            }
            // ✅ ISTNIEJĄCA LOGIKA DLA ZWYKŁYCH UŻYTKOWNIKÓW (bez zmian)
            else if (assignedToMe == null || assignedToMe) {
                // Domyślnie: pokaż TYLKO zadania gdzie użytkownik jest w assignedUsers
                System.out.println("📋 Filtering tasks: ONLY assigned to user " + currentUser.getUsername());
                tasks = taskService.getTasksByAssignedUser(currentUser);
                System.out.println("✅ Found " + tasks.size() + " tasks assigned to user");
            } else if (projectId != null) {
                // assignedToMe=false + projectId: wszystkie zadania z konkretnego projektu (tylko dla członków)
                Project project = projectService.getProjectById(projectId)
                        .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));
                checkProjectAccess(project, currentUser);
                tasks = taskService.getTasksByProject(project);
            } else {
                // assignedToMe=false bez projectId: wszystkie zadania ze wszystkich projektów użytkownika
                List<Project> userProjects = projectService.getProjectsByUser(currentUser);
                tasks = userProjects.stream()
                        .flatMap(p -> taskService.getTasksByProject(p).stream())
                        .distinct()
                        .toList();
            }

            // Apply additional filters
            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> status.equals(t.getStatus()))
                        .toList();
            }

            if (priority != null && !priority.isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> priority.equals(t.getPriority()))
                        .toList();
            }

            List<TaskDto> taskDtos = tasks.stream()
                    .map(taskMapper::toDtoWithStats)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve tasks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/tasks/project/{projectId} - Get tasks by project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

            checkProjectAccess(project, currentUser);

            List<Task> tasks;
            if (status != null && !status.isEmpty()) {
                tasks = taskService.getTasksByProject(project).stream()
                        .filter(t -> status.equals(t.getStatus()))
                        .toList();
            } else {
                tasks = taskService.getTasksByProject(project);
            }

            List<TaskDto> taskDtos = tasks.stream()
                    .map(taskMapper::toDtoWithStats)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("projectId", projectId);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve project tasks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/tasks/my - Get tasks assigned to current user
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyTasks(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            List<Task> tasks = taskService.getTasksByAssignedUser(currentUser);

            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> status.equals(t.getStatus()))
                        .toList();
            }

            List<TaskDto> taskDtos = tasks.stream()
                    .map(taskMapper::toDtoWithStats)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve user tasks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // POST ENDPOINT - Create task
    // ============================================================================

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(
            @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User creator = getCurrentUser(userDetails);

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            // Get project and check access
            Project project = projectService.getProjectById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project with ID " + request.getProjectId() + " not found"));

            checkProjectAccess(project, creator);

            // Check if user can create tasks (not viewer)
            ProjectMember membership = projectMemberService.getProjectMember(project, creator)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this project"));

            if (membership.getRole() == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot create tasks", HttpStatus.FORBIDDEN);
            }

            // Create task
            Task task = new Task();
            task.setTitle(request.getTitle());
            task.setDescription(request.getDescription());
            task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
            task.setStatus("TODO"); // Default status for new tasks
            task.setProject(project);
            task.setCreatedBy(creator);
            task.setDeadline(request.getDeadline());

            // Handle assignment
            if (request.getAssignedToId() != null) {
                User assignedUser = userService.getUserById(request.getAssignedToId())
                        .orElseThrow(() -> new RuntimeException("User with ID " + request.getAssignedToId() + " not found"));
                task.setAssignedTo(assignedUser);
            }

            // Obsługa wielu użytkowników
            if (request.getAssignedUserIds() != null && !request.getAssignedUserIds().isEmpty()) {
                Set<User> assignedUsers = new HashSet<>();
                for (Long userId : request.getAssignedUserIds()) {
                    User assignedUser = userService.getUserById(userId)
                            .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));
                    assignedUsers.add(assignedUser);
                }
                task.setAssignedUsers(assignedUsers);
            }

            Task savedTask = taskService.saveTask(task);
            TaskDto taskDto = taskMapper.toDtoWithStats(savedTask);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task created successfully");
            response.put("data", taskDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to create task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // PUT ENDPOINT - Update task
    // ============================================================================

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            checkTaskAccess(task, currentUser);

            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);

            // Check permissions for status changes
            if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
                if (userRole == ProjectRole.VIEWER) {
                    return createErrorResponse(
                            "Viewers cannot change task status. Please use status change request endpoint: POST /api/v1/status-requests",
                            HttpStatus.FORBIDDEN
                    );
                }
            }

            // Update fields
            if (request.getTitle() != null) {
                task.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                task.setDescription(request.getDescription());
            }
            if (request.getStatus() != null) {
                task.setStatus(request.getStatus());
            }
            if (request.getPriority() != null) {
                task.setPriority(request.getPriority());
            }
            if (request.getDeadline() != null) {
                task.setDeadline(request.getDeadline());
            }

            // Handle assignment updates
            if (request.getAssignedToId() != null) {
                User assignedUser = userService.getUserById(request.getAssignedToId())
                        .orElseThrow(() -> new RuntimeException("User with ID " + request.getAssignedToId() + " not found"));
                task.setAssignedTo(assignedUser);
            }

            // Obsługa wielu użytkowników
            if (request.getAssignedUserIds() != null) {
                Set<User> assignedUsers = new HashSet<>();
                for (Long userId : request.getAssignedUserIds()) {
                    User assignedUser = userService.getUserById(userId)
                            .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));
                    assignedUsers.add(assignedUser);
                }
                task.setAssignedUsers(assignedUsers);
            }

            Task updatedTask = taskService.saveTask(task);
            TaskDto taskDto = taskMapper.toDtoWithStats(updatedTask);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task updated successfully");
            response.put("data", taskDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to update task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // DELETE ENDPOINT - Delete task
    // ============================================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            checkTaskAccess(task, currentUser);

            ProjectRole userRole = getUserRoleInProject(task.getProject(), currentUser);
            if (userRole == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot delete tasks", HttpStatus.FORBIDDEN);
            }

            taskService.deleteTask(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}