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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;
    private final TaskMapper taskMapper;

    public TaskApiController(TaskService taskService,
                             ProjectService projectService,
                             ProjectMemberService projectMemberService,
                             UserService userService,
                             TaskMapper taskMapper) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
        this.taskMapper = taskMapper;
    }

    // GET /api/v1/tasks - Get all tasks (filtered by user access)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assignedToMe", defaultValue = "false") boolean assignedToMe) {

        try {
            User currentUser = getTestUser();
            List<Task> tasks;

            if (projectId != null) {
                // Tasks for specific project
                Project project = projectService.getProjectById(projectId)
                        .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

                // Check user access to project
                checkProjectAccess(project, currentUser);
                tasks = taskService.getTasksByProject(project);

            } else if (assignedToMe) {
                // Tasks assigned to current user
                tasks = taskService.getTasksByAssignedUser(currentUser);

            } else {
                // All tasks user has access to (through projects)
                List<Project> userProjects = projectMemberService.getUserProjects(currentUser);
                tasks = userProjects.stream()
                        .flatMap(pm -> taskService.getTasksByProject(pm.getProject()).stream())
                        .distinct()
                        .collect(Collectors.toList());
            }

            // Apply filters
            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(task -> status.equals(task.getStatus()))
                        .collect(Collectors.toList());
            }

            if (priority != null && !priority.isEmpty()) {
                tasks = tasks.stream()
                        .filter(task -> priority.equals(task.getPriority()))
                        .collect(Collectors.toList());
            }

            List<TaskDto> taskDtos = taskMapper.toDtoWithStats(tasks);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("count", taskDtos.size());
            response.put("testUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve tasks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/tasks/{id} - Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();
            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            // Check user access to project
            checkProjectAccess(task.getProject(), currentUser);

            TaskDto taskDto = taskMapper.toDtoWithStats(task);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task retrieved successfully");
            response.put("data", taskDto);
            response.put("testUser", currentUser.getUsername());

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

    // POST /api/v1/tasks - Create new task
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody CreateTaskRequest request) {

        try {
            User creator = getTestUser();

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
            TaskDto taskDto = taskMapper.toDtoWithStats(savedTask);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task created successfully");
            response.put("data", taskDto);
            response.put("testUser", creator.getUsername());

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

    // PUT /api/v1/tasks/{id} - Update task
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request) {

        try {
            User currentUser = getTestUser();

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

            // Update task fields
            if (request.getTitle() != null) {
                task.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                task.setDescription(request.getDescription());
            }
            if (request.getStatus() != null) {
                task.setStatus(request.getStatus());

                // Set completion time if task is completed
                if ("COMPLETED".equals(request.getStatus()) && task.getCompletedAt() == null) {
                    task.setCompletedAt(java.time.LocalDateTime.now());
                } else if (!"COMPLETED".equals(request.getStatus())) {
                    task.setCompletedAt(null);
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

            Task updatedTask = taskService.saveTask(task);
            TaskDto taskDto = taskMapper.toDtoWithStats(updatedTask);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task updated successfully");
            response.put("data", taskDto);
            response.put("testUser", currentUser.getUsername());

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

    // DELETE /api/v1/tasks/{id} - Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

            // Check user access to project
            checkProjectAccess(task.getProject(), currentUser);

            // Check if user can delete tasks (admin or creator)
            ProjectMember membership = projectMemberService.getProjectMember(task.getProject(), currentUser)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this project"));

            if (membership.getRole() == ProjectRole.VIEWER) {
                return createErrorResponse("Viewers cannot delete tasks", HttpStatus.FORBIDDEN);
            }

            // Only task creator or project admin can delete
            if (!task.getCreatedBy().equals(currentUser) && membership.getRole() != ProjectRole.ADMIN) {
                return createErrorResponse("Only task creator or project admin can delete this task", HttpStatus.FORBIDDEN);
            }

            taskService.deleteTask(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task deleted successfully");
            response.put("testUser", currentUser.getUsername());

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

    // GET /api/v1/tasks/project/{projectId} - Get tasks for specific project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status) {

        try {
            User currentUser = getTestUser();

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

            checkProjectAccess(project, currentUser);

            List<Task> tasks = taskService.getTasksByProject(project);

            // Apply status filter if provided
            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(task -> status.equals(task.getStatus()))
                        .collect(Collectors.toList());
            }

            List<TaskDto> taskDtos = taskMapper.toDtoWithStats(tasks);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("count", taskDtos.size());
            response.put("projectId", projectId);
            response.put("testUser", currentUser.getUsername());

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
            @RequestParam(value = "status", required = false) String status) {

        try {
            User currentUser = getTestUser();

            List<Task> tasks = taskService.getTasksByAssignedUser(currentUser);

            // Apply status filter if provided
            if (status != null && !status.isEmpty()) {
                tasks = tasks.stream()
                        .filter(task -> status.equals(task.getStatus()))
                        .collect(Collectors.toList());
            }

            List<TaskDto> taskDtos = taskMapper.toDtoWithStats(tasks);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "My tasks retrieved successfully");
            response.put("data", taskDtos);
            response.put("count", taskDtos.size());
            response.put("testUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve my tasks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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

    private void checkProjectAccess(Project project, User user) {
        if (user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            Optional<ProjectMember> memberOpt = projectMemberService.getProjectMember(project, user);
            if (memberOpt.isEmpty()) {
                throw new IllegalArgumentException("Access denied to project with ID " + project.getId());
            }
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}