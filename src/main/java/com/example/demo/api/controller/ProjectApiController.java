// src/main/java/com/example/demo/api/controller/ProjectApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.CreateProjectRequest;
import com.example.demo.api.dto.request.UpdateProjectRequest;
import com.example.demo.api.dto.response.ProjectDto;
import com.example.demo.api.dto.response.ProjectMemberDto;
import com.example.demo.api.mapper.ProjectMapper;
import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class ProjectApiController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final TaskService taskService;
    private final UserService userService;
    private final ProjectMapper projectMapper;

    public ProjectApiController(ProjectService projectService,
                                ProjectMemberService projectMemberService,
                                TaskService taskService,
                                UserService userService,
                                ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.taskService = taskService;
        this.userService = userService;
        this.projectMapper = projectMapper;
    }

    // ✅ Używa prawdziwego zalogowanego użytkownika
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    private void checkProjectAccess(Project project, User user) {
        Optional<ProjectMember> membership = projectMemberService.getProjectMember(project, user);
        if (membership.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
    }

    private boolean isProjectAdmin(Project project, User user) {
        return projectMemberService.getProjectMember(project, user)
                .map(member -> member.getRole() == ProjectRole.ADMIN)
                .orElse(false);
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

    // GET /api/v1/projects - Get all projects (filtered by user access)
    // ✅ POPRAWIONE: zwraca ProjectDto z memberCount i taskCount
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProjects(
            @RequestParam(value = "includeAll", defaultValue = "false") boolean includeAll,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            List<Project> projects;

            // Super admin can see all projects if requested
            if (includeAll && currentUser.getSystemRole() == SystemRole.SUPER_ADMIN) {
                projects = projectService.getAllProjects();
            } else {
                // Regular users see only their projects
                List<ProjectMember> memberships = projectMemberService.getUserProjects(currentUser);
                projects = memberships.stream()
                        .map(ProjectMember::getProject)
                        .distinct()
                        .toList();
            }

            // ✅ KLUCZOWA ZMIANA: Konwertuj na ProjectDto ze statystykami
            List<ProjectDto> projectDtos = projects.stream()
                    .map(project -> {
                        ProjectDto dto = projectMapper.toDto(project);

                        // Policz członków
                        List<ProjectMember> members = projectMemberService.getProjectMembers(project);
                        dto.setMemberCount(members.size());

                        // Policz zadania
                        List<Task> tasks = taskService.getTasksByProject(project);
                        dto.setTaskCount(tasks.size());

                        // Policz ukończone zadania
                        long completedTasks = tasks.stream()
                                .filter(t -> "COMPLETED".equals(t.getStatus()))
                                .count();
                        dto.setCompletedTaskCount((int) completedTasks);

                        // Ustaw status projektu
                        if (tasks.isEmpty()) {
                            dto.setStatus("planning");
                        } else if (completedTasks == tasks.size()) {
                            dto.setStatus("completed");
                        } else {
                            dto.setStatus("active");
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Projects retrieved successfully");
            response.put("data", projectDtos);  // ✅ Zwracamy ProjectDto zamiast surowych Project
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve projects: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/projects/{id} - Get project by ID with details
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + id + " not found"));

            checkProjectAccess(project, currentUser);

            // Pobierz członków projektu
            List<ProjectMember> members = projectMemberService.getProjectMembers(project);

            // Konwertuj na DTO z członkami
            ProjectDto projectDto = projectMapper.toDtoWithMembers(project, members);

            // Dodaj statystyki
            List<Task> tasks = taskService.getTasksByProject(project);
            projectDto.setMemberCount(members.size());
            projectDto.setTaskCount(tasks.size());

            long completedTasks = tasks.stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()))
                    .count();
            projectDto.setCompletedTaskCount((int) completedTasks);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project retrieved successfully");
            response.put("data", projectDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/projects/{id}/members - Get project members
    @GetMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> getProjectMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + id + " not found"));

            checkProjectAccess(project, currentUser);

            List<ProjectMember> members = projectMemberService.getProjectMembers(project);

            // Konwertuj na DTO
            List<ProjectMemberDto> memberDtos = members.stream()
                    .map(projectMapper::toMemberDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project members retrieved successfully");
            response.put("data", memberDtos);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to retrieve project members: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // POST ENDPOINTS
    // ============================================================================

    // POST /api/v1/projects - Create new project
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(
            @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User creator = getCurrentUser(userDetails);

            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return createErrorResponse("Project name is required", HttpStatus.BAD_REQUEST);
            }

            // Create project
            Project project = projectService.createProject(
                    request.getName(),
                    request.getDescription() != null ? request.getDescription() : "",
                    creator
            );

            // Twórca automatycznie staje się adminem
            projectMemberService.addMemberToProject(project, creator, ProjectRole.ADMIN);

            // Konwertuj na DTO ze statystykami
            ProjectDto projectDto = projectMapper.toDto(project);
            projectDto.setMemberCount(1); // Twórca
            projectDto.setTaskCount(0); // Nowy projekt
            projectDto.setStatus("planning");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project created successfully");
            response.put("data", projectDto);
            response.put("currentUser", creator.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to create project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/projects/{id}/members - Add member to project
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMemberToProject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + id + " not found"));

            checkProjectAccess(project, currentUser);

            // Check admin permissions
            if (!isProjectAdmin(project, currentUser)) {
                return createErrorResponse("Only project administrators can add members", HttpStatus.FORBIDDEN);
            }

            Long userId = Long.valueOf(request.get("userId").toString());
            String roleStr = request.get("role").toString();
            ProjectRole role = ProjectRole.valueOf(roleStr);

            User userToAdd = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            ProjectMember newMember = projectMemberService.addMemberToProject(project, userToAdd, role);

            ProjectMemberDto memberDto = projectMapper.toMemberDto(newMember);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member added to project successfully");
            response.put("data", memberDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to add member: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // PUT ENDPOINTS
    // ============================================================================

    // PUT /api/v1/projects/{id} - Update project
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable Long id,
            @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + id + " not found"));

            checkProjectAccess(project, currentUser);

            // Only admins can update projects
            if (!isProjectAdmin(project, currentUser)) {
                return createErrorResponse("Only project administrators can update projects", HttpStatus.FORBIDDEN);
            }

            // Update project
            if (request.getName() != null) {
                project.setName(request.getName());
            }
            if (request.getDescription() != null) {
                project.setDescription(request.getDescription());
            }
            if (request.getDeadline() != null) {
                project.setDeadline(request.getDeadline());
            }

            Project updatedProject = projectService.updateProject(project);

            // Konwertuj na DTO ze statystykami
            ProjectDto projectDto = projectMapper.toDto(updatedProject);
            List<ProjectMember> members = projectMemberService.getProjectMembers(updatedProject);
            List<Task> tasks = taskService.getTasksByProject(updatedProject);

            projectDto.setMemberCount(members.size());
            projectDto.setTaskCount(tasks.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project updated successfully");
            response.put("data", projectDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to update project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/projects/{projectId}/members/{memberId}/role - Change member role
    // ============================================================================
// POPRAWIONA METODA changeMemberRole w ProjectApiController
// Wklej tę metodę do src/main/java/com/example/demo/api/controller/ProjectApiController.java
// Zastąp istniejącą metodę changeMemberRole
// ============================================================================

    // PUT /api/v1/projects/{projectId}/members/{userId}/role - Change member role
    // ✅ POPRAWIONE: Teraz przyjmuje userId zamiast memberId
    @PutMapping("/{projectId}/members/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,  // ✅ ZMIENIONE z memberId na userId
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

            // ✅ POPRAWIONE: SUPER_ADMIN może zmieniać role w dowolnym projekcie
            boolean isSuperAdmin = currentUser.getSystemRole() == SystemRole.SUPER_ADMIN;

            if (!isSuperAdmin) {
                // Zwykli użytkownicy muszą być członkami projektu
                checkProjectAccess(project, currentUser);

                // I muszą być adminami projektu
                if (!isProjectAdmin(project, currentUser)) {
                    return createErrorResponse("Only project administrators can change member roles", HttpStatus.FORBIDDEN);
                }
            }

            String newRoleStr = request.get("role");
            ProjectRole newRole = ProjectRole.valueOf(newRoleStr);

            // ✅ NOWA LOGIKA: Znajdź użytkownika
            User userToUpdate = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Znajdź członkostwo
            ProjectMember memberToUpdate = projectMemberService.getProjectMember(project, userToUpdate)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this project"));

            // Sprawdź czy to nie twórca projektu próbujący zmienić swoją rolę na nie-admin
            if (project.getCreatedBy().equals(userToUpdate) && newRole != ProjectRole.ADMIN) {
                return createErrorResponse("Cannot change project creator's role from ADMIN", HttpStatus.FORBIDDEN);
            }

            // Zaktualizuj rolę używając istniejącej metody
            ProjectMember updatedMember = projectMemberService.changeMemberRole(memberToUpdate.getId(), newRole);
            ProjectMemberDto memberDto = projectMapper.toMemberDto(updatedMember);

            // Log dla debugowania
            System.out.println("✅ Role of " + userToUpdate.getUsername() + " changed to " + newRole +
                    " in project " + project.getName() + " by " + currentUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member role updated successfully");
            response.put("data", memberDto);
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to update member role: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================================================
    // DELETE ENDPOINTS
    // ============================================================================

    // DELETE /api/v1/projects/{id} - Delete project
    // ============================================================================
// POPRAWIONA METODA deleteProject w ProjectApiController
// Wklej tę metodę do src/main/java/com/example/demo/api/controller/ProjectApiController.java
// Zastąp istniejącą metodę deleteProject
// ============================================================================

    // DELETE /api/v1/projects/{id} - Delete project
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + id + " not found"));

            // ✅ POPRAWIONA LOGIKA: SUPER_ADMIN może usuwać dowolne projekty
            // Sprawdzamy czy użytkownik jest SUPER_ADMIN lub adminem projektu
            boolean isSuperAdmin = currentUser.getSystemRole() == SystemRole.SUPER_ADMIN;
            boolean isProjectAdministrator = isProjectAdmin(project, currentUser);

            if (!isSuperAdmin && !isProjectAdministrator) {
                // Jeśli użytkownik nie jest ani SUPER_ADMIN ani adminem projektu
                if (projectMemberService.getProjectMember(project, currentUser).isEmpty()) {
                    return createErrorResponse("You are not a member of this project", HttpStatus.FORBIDDEN);
                } else {
                    return createErrorResponse("Only project administrators can delete projects", HttpStatus.FORBIDDEN);
                }
            }

            // Log dla debugowania
            if (isSuperAdmin) {
                System.out.println("🔥 SUPER_ADMIN " + currentUser.getUsername() + " deleting project: " + project.getName());
            } else {
                System.out.println("👨‍💼 Project admin " + currentUser.getUsername() + " deleting project: " + project.getName());
            }

            projectService.deleteProject(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project deleted successfully");
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // DELETE /api/v1/projects/{projectId}/members/{memberId} - Remove member from project
    // ============================================================================
// POPRAWIONA METODA removeMemberFromProject w ProjectApiController
// Wklej tę metodę do src/main/java/com/example/demo/api/controller/ProjectApiController.java
// Zastąp istniejącą metodę removeMemberFromProject
// ============================================================================

    // DELETE /api/v1/projects/{projectId}/members/{userId} - Remove member from project
    // ✅ POPRAWIONE: Teraz przyjmuje userId zamiast memberId
    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMemberFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,  // ✅ ZMIENIONE z memberId na userId
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

            // ✅ POPRAWIONE: SUPER_ADMIN może usuwać członków z dowolnego projektu
            boolean isSuperAdmin = currentUser.getSystemRole() == SystemRole.SUPER_ADMIN;

            if (!isSuperAdmin) {
                // Zwykli użytkownicy muszą być członkami projektu
                checkProjectAccess(project, currentUser);

                // I muszą być adminami projektu
                if (!isProjectAdmin(project, currentUser)) {
                    return createErrorResponse("Only project administrators can remove members", HttpStatus.FORBIDDEN);
                }
            }

            // ✅ NOWA LOGIKA: Znajdź użytkownika do usunięcia
            User userToRemove = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Sprawdź czy użytkownik jest członkiem projektu
            Optional<ProjectMember> membershipOpt = projectMemberService.getProjectMember(project, userToRemove);
            if (membershipOpt.isEmpty()) {
                return createErrorResponse("User is not a member of this project", HttpStatus.NOT_FOUND);
            }

            // Nie można usunąć twórcy projektu
            if (project.getCreatedBy().equals(userToRemove)) {
                return createErrorResponse("Cannot remove project creator", HttpStatus.FORBIDDEN);
            }

            // Usuń członka używając istniejącej metody
            projectMemberService.removeMemberFromProject(project, userToRemove);

            // Log dla debugowania
            System.out.println("✅ User " + userToRemove.getUsername() + " removed from project " + project.getName() +
                    " by " + currentUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member removed from project successfully");
            response.put("currentUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to remove member: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}