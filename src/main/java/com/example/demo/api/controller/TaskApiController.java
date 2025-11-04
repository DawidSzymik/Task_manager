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
    // ✅ KLUCZOWA ZMIANA: domyślnie pokazuje TYLKO zadania przypisane do zalogowanego użytkownika
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

            // ✅ KLUCZOWA LOGIKA:
            // - assignedToMe=true LUB brak parametru → tylko zadania przypisane do użytkownika
            // - assignedToMe=false → wszystkie zadania z projektów użytkownika
            if (assignedToMe == null || assignedToMe) {
                // Domyślnie: pokaż TYLKO zadania gdzie użytkownik jest w assignedUsers
                System.out.println("📋 Filtruję zadania: TYLKO przypisane do użytkownika " + currentUser.getUsername());
                tasks = taskService.getTasksByAssignedUser(currentUser);
                System.out.println("✅ Znaleziono " + tasks.size() + " zadań przypisanych do użytkownika");
            } else if (projectId != null) {
                // assignedToMe=false + projectId: wszystkie zadania z konkretnego projektu
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
            task.setStatus("NEW");
            task.setDeadline(request.getDeadline());
            task.setProject(project);
            task.setCreatedBy(creator);

            // Handle assignments
            if (request.getAssignedToId() != null) {
                User assignedUser = userService.getUserById(request.getAssignedToId())
                        .orElseThrow(() -> new RuntimeException("User with ID " + request.getAssignedToId() + " not found"));
                task.setAssignedTo(assignedUser);
            }

            if (request.getAssignedUserIds() != null && !request.getAssignedUserIds().isEmpty()) {
                for (Long userId : request.getAssignedUserIds()) {
                    User assignedUser = userService.getUserById(userId)
                            .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));
                    task.getAssignedUsers().add(assignedUser);
                }
            }

            Task savedTask = taskService.saveTask(task);

            System.out.println("\n========================================");
            System.out.println("🔵 Zadanie zapisane - rozpoczynam wysyłanie powiadomień");
            System.out.println("Zadanie: " + savedTask.getTitle() + " (ID: " + savedTask.getId() + ")");
            System.out.println("Twórca: " + creator.getUsername() + " (ID: " + creator.getId() + ")");
            System.out.println("Projekt: " + project.getName() + " (ID: " + project.getId() + ")");

            // Sprawdź przypisanych użytkowników
            Set<User> assignedUsers = savedTask.getAssignedUsers();
            System.out.println("📋 Przypisanych użytkowników: " + (assignedUsers != null ? assignedUsers.size() : 0));
            if (assignedUsers != null && !assignedUsers.isEmpty()) {
                for (User u : assignedUsers) {
                    System.out.println("  - " + u.getUsername() + " (ID: " + u.getId() + ")");
                }
            }

            // ✅ WYSYŁANIE POWIADOMIEŃ - nowe zadanie w projekcie
            try {
                System.out.println("\n🔔 Pobieram członków projektu...");
                List<ProjectMember> projectMembers = projectMemberService.getProjectMembers(project);
                System.out.println("Członków projektu: " + projectMembers.size());

                int notificationsSent = 0;
                for (ProjectMember member : projectMembers) {
                    User memberUser = member.getUser();
                    System.out.println("\n  👤 Sprawdzam członka: " + memberUser.getUsername() +
                            " (ID: " + memberUser.getId() + ", rola: " + member.getRole() + ")");

                    // Powiadom tylko członków projektu (nie twórcy zadania)
                    if (memberUser.equals(creator)) {
                        System.out.println("  ⏭️ Pomijam - to twórca zadania");
                        continue;
                    }

                    System.out.println("  📤 Wysyłam powiadomienie...");
                    try {
                        Notification notification = notificationService.createNotification(
                                memberUser,
                                "📋 Nowe zadanie w projekcie",
                                creator.getUsername() + " utworzył zadanie \"" + task.getTitle() +
                                        "\" w projekcie \"" + project.getName() + "\"",
                                NotificationType.TASK_ASSIGNED,
                                savedTask.getId(),
                                "/tasks/view/" + savedTask.getId()
                        );
                        System.out.println("  ✅ Powiadomienie wysłane (ID: " + notification.getId() + ")");
                        notificationsSent++;
                    } catch (Exception e) {
                        System.err.println("  ❌ Błąd wysyłania powiadomienia: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                System.out.println("\n✅ Wysłano łącznie " + notificationsSent + " powiadomień o nowym zadaniu");
            } catch (Exception e) {
                System.err.println("❌ KRYTYCZNY BŁĄD w sekcji powiadomień: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("========================================\n");

            TaskDto taskDto = taskMapper.toDtoWithStats(savedTask);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task created successfully");
            response.put("data", taskDto);
            response.put("currentUser", creator.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            if (!request.hasUpdates()) {
                return createErrorResponse("No updates provided", HttpStatus.BAD_REQUEST);
            }

            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            // Check user access to project
            checkProjectAccess(task.getProject(), currentUser);

            // Check if user can modify tasks
            ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), currentUser)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this project"));

            if (membership.getRole() == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot modify tasks", HttpStatus.FORBIDDEN);
            }

            // ✅ NOWA LOGIKA: Sprawdź, czy próbuje się zmienić status
            if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
                // Tylko ADMIN może bezpośrednio zmieniać status
                if (membership.getRole() != ProjectRole.ADMIN) {
                    return createErrorResponse(
                            "Members cannot change task status directly. Please use status change request endpoint: POST /api/v1/status-requests",
                            HttpStatus.FORBIDDEN
                    );
                }
            }

            // Update task fields
            if (request.getTitle() != null) {
                task.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                task.setDescription(request.getDescription());
            }
            // ⚠️ WAŻNE: Status może być zmieniony tylko przez ADMIN (sprawdzono wyżej)
            // ✅ Blokuj zmianę statusu dla MEMBER
            if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
                if (membership.getRole() != ProjectRole.ADMIN) {
                    return createErrorResponse(
                            "Members cannot change task status directly. Please use status change request endpoint: POST /api/v1/status-requests",
                            HttpStatus.FORBIDDEN
                    );
                }
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

            checkProjectAccess(task.getProject(), currentUser);

            ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), currentUser)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this project"));

            if (membership.getRole() != ProjectRole.ADMIN) {
                return createErrorResponse("Only admins can delete tasks", HttpStatus.FORBIDDEN);
            }

            taskService.deleteTask(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task deleted successfully");
            response.put("currentUser", currentUser.getUsername());

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