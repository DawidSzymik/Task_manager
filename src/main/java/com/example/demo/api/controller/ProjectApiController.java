// src/main/java/com/example/demo/api/controller/ProjectApiController.java
package com.example.demo.api.controller;

import com.example.demo.model.Project;
import com.example.demo.model.ProjectMember;
import com.example.demo.model.User;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.SystemRole;
import com.example.demo.service.ProjectMemberService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/projects")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class ProjectApiController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    public ProjectApiController(ProjectService projectService,
                                ProjectMemberService projectMemberService,
                                UserService userService) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
    }

    // GET /api/v1/projects - Get all projects (filtered by user access)
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Projects retrieved successfully");
            response.put("data", projects);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve projects: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/projects/{id} - Get project by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project retrieved successfully");
            response.put("data", project);

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

    // POST /api/v1/projects - Create new project
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // ✅ TUTAJ JEST NAPRAWA - używamy prawdziwego zalogowanego użytkownika jako creator!
            User creator = getCurrentUser(userDetails);

            System.out.println("🔍 Creating project by user: " + creator.getUsername());

            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Project name is required", HttpStatus.BAD_REQUEST);
            }

            // Create project with actual logged-in user as creator
            Project project = projectService.createProject(name, description != null ? description : "", creator);

            // Add creator as admin
            projectMemberService.addMemberToProject(project, creator, ProjectRole.ADMIN);

            System.out.println("✅ Project created successfully by: " + project.getCreatedBy().getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project created successfully");
            response.put("data", project);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to create project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/projects/{id} - Update project
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            // Check if user has admin rights or is creator
            if (!canModifyProject(project, currentUser)) {
                return createErrorResponse("Insufficient permissions to modify this project", HttpStatus.FORBIDDEN);
            }

            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Project name is required", HttpStatus.BAD_REQUEST);
            }

            // Update project
            project.setName(name);
            project.setDescription(description != null ? description : "");
            Project updatedProject = projectService.saveProject(project);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project updated successfully");
            response.put("data", updatedProject);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to update project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/projects/{id} - Delete project
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            // Only creator or super admin can delete project
            if (!project.getCreatedBy().equals(currentUser) &&
                    currentUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                return createErrorResponse("Only project creator or super admin can delete this project", HttpStatus.FORBIDDEN);
            }

            // Use the admin delete method which handles cascading
            projectService.deleteProjectByAdmin(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to delete project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/projects/{id}/members - Get project members
    @GetMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> getProjectMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            List<ProjectMember> members = projectMemberService.getProjectMembers(project);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project members retrieved successfully");
            response.put("data", members);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to retrieve project members: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            Project project = getProjectAndCheckAccess(id, currentUser);

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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member added to project successfully");
            response.put("data", newMember);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to add member: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/projects/{id}/members/{userId} - Remove member from project
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMemberFromProject(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            // Check admin permissions
            if (!isProjectAdmin(project, currentUser)) {
                return createErrorResponse("Only project administrators can remove members", HttpStatus.FORBIDDEN);
            }

            User userToRemove = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Cannot remove project creator
            if (project.getCreatedBy().equals(userToRemove)) {
                return createErrorResponse("Cannot remove project creator", HttpStatus.BAD_REQUEST);
            }

            projectMemberService.removeMemberFromProject(project, userToRemove);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member removed from project successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to remove member: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/projects/{id}/members/{userId}/role - Change member role
    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            Project project = getProjectAndCheckAccess(id, currentUser);

            // Check admin permissions
            if (!isProjectAdmin(project, currentUser)) {
                return createErrorResponse("Only project administrators can change member roles", HttpStatus.FORBIDDEN);
            }

            User targetUser = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            String roleStr = request.get("role");
            if (roleStr == null) {
                return createErrorResponse("Role is required", HttpStatus.BAD_REQUEST);
            }

            ProjectRole newRole = ProjectRole.valueOf(roleStr);

            projectMemberService.changeUserRole(project, targetUser, newRole);

            // Get updated member info
            ProjectMember updatedMember = projectMemberService.getProjectMember(project, targetUser)
                    .orElseThrow(() -> new RuntimeException("Member not found after role change"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member role changed successfully");
            response.put("data", updatedMember);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to change member role: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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

    private Project getProjectAndCheckAccess(Long projectId, User user) {
        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found"));

        // Check if user has access to this project
        if (user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            Optional<ProjectMember> memberOpt = projectMemberService.getProjectMember(project, user);
            if (memberOpt.isEmpty()) {
                throw new IllegalArgumentException("Access denied to project with ID " + projectId);
            }
        }

        return project;
    }

    private boolean canModifyProject(Project project, User user) {
        // Creator can always modify
        if (project.getCreatedBy().equals(user)) {
            return true;
        }

        // Super admin can modify any project
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return true;
        }

        // Project admin can modify
        return isProjectAdmin(project, user);
    }

    private boolean isProjectAdmin(Project project, User user) {
        Optional<ProjectMember> memberOpt = projectMemberService.getProjectMember(project, user);
        return memberOpt.isPresent() && memberOpt.get().getRole() == ProjectRole.ADMIN;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}