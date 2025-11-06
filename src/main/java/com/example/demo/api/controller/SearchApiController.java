// src/main/java/com/example/demo/api/controller/SearchApiController.java
package com.example.demo.api.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SearchApiController {

    private final UserService userService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    public SearchApiController(UserService userService,
                               TaskService taskService,
                               ProjectService projectService,
                               ProjectMemberService projectMemberService) {
        this.userService = userService;
        this.taskService = taskService;
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
    }

    /**
     * Global search endpoint
     * GET /api/v1/search?q=query&limit=10
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {

        try {
            // Get current user (using first user for testing)
            List<User> allUsers = userService.getAllUsers();
            if (allUsers.isEmpty()) {
                return createErrorResponse("No users found", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            User currentUser = allUsers.get(0);

            // If query is empty, return empty results
            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("query", "");
                response.put("users", Collections.emptyList());
                response.put("tasks", Collections.emptyList());
                response.put("projects", Collections.emptyList());
                response.put("totalResults", 0);
                return ResponseEntity.ok(response);
            }

            String searchQuery = query.toLowerCase().trim();

            // Search Users
            List<Map<String, Object>> users = searchUsers(searchQuery, limit);

            // Search Tasks (only tasks in projects user has access to)
            List<Map<String, Object>> tasks = searchTasks(currentUser, searchQuery, limit);

            // Search Projects (only projects user has access to)
            List<Map<String, Object>> projects = searchProjects(currentUser, searchQuery, limit);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("users", users);
            response.put("tasks", tasks);
            response.put("projects", projects);
            response.put("totalResults", users.size() + tasks.size() + projects.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Search failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Search users by username or email
     * ✅ NAPRAWIONE - Dodano avatarUrl i hasAvatar
     */
    private List<Map<String, Object>> searchUsers(String query, int limit) {
        return userService.getAllUsers().stream()
                .filter(user -> user.isActive() &&
                        (user.getUsername().toLowerCase().contains(query) ||
                                (user.getEmail() != null && user.getEmail().toLowerCase().contains(query))))
                .limit(limit)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("systemRole", user.getSystemRole().name());
                    userMap.put("type", "user");
                    userMap.put("url", "/users/" + user.getId());

                    // ✅ DODANE - Avatar URL i hasAvatar
                    if (user.hasAvatar()) {
                        userMap.put("avatarUrl", "/api/v1/users/" + user.getId() + "/avatar");
                        userMap.put("hasAvatar", true);
                    } else {
                        userMap.put("hasAvatar", false);
                    }

                    // Opcjonalnie dodaj fullName jeśli istnieje
                    if (user.getFullName() != null) {
                        userMap.put("fullName", user.getFullName());
                    }

                    return userMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Search tasks by title or description
     */
    private List<Map<String, Object>> searchTasks(User currentUser, String query, int limit) {
        // Get all projects user has access to
        List<ProjectMember> userMemberships = projectMemberService.getUserProjects(currentUser);
        List<Project> accessibleProjects = userMemberships.stream()
                .map(ProjectMember::getProject)
                .collect(Collectors.toList());

        // Search tasks in accessible projects
        return accessibleProjects.stream()
                .flatMap(project -> taskService.getTasksByProject(project).stream())
                .filter(task -> task.getTitle().toLowerCase().contains(query) ||
                        (task.getDescription() != null && task.getDescription().toLowerCase().contains(query)))
                .limit(limit)
                .map(task -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("title", task.getTitle());
                    taskMap.put("description", task.getDescription() != null ?
                            (task.getDescription().length() > 100 ?
                                    task.getDescription().substring(0, 100) + "..." :
                                    task.getDescription()) :
                            null);
                    taskMap.put("status", task.getStatus());
                    taskMap.put("priority", task.getPriority());
                    taskMap.put("projectName", task.getProject().getName());
                    taskMap.put("projectId", task.getProject().getId());
                    taskMap.put("type", "task");
                    taskMap.put("url", "/projects/" + task.getProject().getId() + "/tasks/" + task.getId());
                    return taskMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Search projects by name or description
     */
    private List<Map<String, Object>> searchProjects(User currentUser, String query, int limit) {
        // Get all projects user has access to
        List<ProjectMember> userMemberships = projectMemberService.getUserProjects(currentUser);

        return userMemberships.stream()
                .map(ProjectMember::getProject)
                .filter(project -> project.getName().toLowerCase().contains(query) ||
                        (project.getDescription() != null && project.getDescription().toLowerCase().contains(query)))
                .limit(limit)
                .map(project -> {
                    Map<String, Object> projectMap = new HashMap<>();
                    projectMap.put("id", project.getId());
                    projectMap.put("name", project.getName());
                    projectMap.put("description", project.getDescription() != null ?
                            (project.getDescription().length() > 100 ?
                                    project.getDescription().substring(0, 100) + "..." :
                                    project.getDescription()) :
                            null);
                    projectMap.put("memberCount", project.getMembers() != null ? project.getMembers().size() : 0);
                    projectMap.put("type", "project");
                    projectMap.put("url", "/projects/" + project.getId());
                    return projectMap;
                })
                .collect(Collectors.toList());
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}