// src/main/java/com/example/demo/api/controller/TeamApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.CreateTeamRequest;
import com.example.demo.api.dto.request.UpdateTeamRequest;
import com.example.demo.api.dto.response.TeamDto;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.api.mapper.TeamMapper;
import com.example.demo.api.mapper.UserMapper;
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
@RequestMapping("/api/v1/teams")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TeamApiController {

    private final TeamService teamService;
    private final UserService userService;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final ProjectMemberService projectMemberService;  // ✅ DODAJ
    private final ProjectService projectService;              // ✅ DODAJ
    private final TaskService taskService;

    public TeamApiController(TeamService teamService,
                             UserService userService,
                             TeamMapper teamMapper,
                             ProjectMemberService projectMemberService,  // ✅ DODAJ
                             ProjectService projectService,              // ✅ DODAJ
                             TaskService taskService,
                             UserMapper userMapper) {
        this.teamService = teamService;
        this.userService = userService;
        this.teamMapper = teamMapper;
        this.userMapper = userMapper;
        this.projectMemberService = projectMemberService;  // ✅ DODAJ
        this.projectService = projectService;              // ✅ DODAJ
        this.taskService = taskService;
    }

    // GET /api/v1/teams - Get all teams
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTeams(
            @RequestParam(value = "myTeams", defaultValue = "false") boolean myTeams) {

        try {
            User currentUser = getTestUser();
            List<Team> teams;

            if (myTeams) {
                // Get only user's teams
                teams = teamService.getTeamsByUser(currentUser);
            } else {
                // Get all teams (super admin or all public teams)
                if (currentUser.getSystemRole() == SystemRole.SUPER_ADMIN) {
                    teams = teamService.getAllTeams();
                } else {
                    teams = teamService.getTeamsByUser(currentUser);
                }
            }

            List<TeamDto> teamDtos = teamMapper.toDtoWithPermissions(teams, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Teams retrieved successfully");
            response.put("data", teamDtos);
            response.put("totalTeams", teams.size());
            response.put("testUser", currentUser.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve teams: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/teams/{id} - Get team by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTeamById(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + id + " not found"));

            // Check if user has access to this team
            checkTeamAccess(team, currentUser);

            // ✅ NOWA LOGIKA: Policz projekty i zadania dla zespołu
            int projectCount = 0;
            int taskCount = 0;

            if (team.getMembers() != null && !team.getMembers().isEmpty()) {
                // Zbierz wszystkie projekty członków zespołu
                java.util.Set<Long> projectIds = new java.util.HashSet<>();

                for (User member : team.getMembers()) {
                    List<com.example.demo.model.ProjectMember> memberProjects =
                            projectMemberService.getUserProjects(member);

                    for (com.example.demo.model.ProjectMember pm : memberProjects) {
                        if (pm.getProject() != null) {
                            projectIds.add(pm.getProject().getId());
                        }
                    }
                }

                projectCount = projectIds.size();

                // Policz wszystkie zadania przypisane do projektów zespołu
                for (Long projectId : projectIds) {
                    com.example.demo.model.Project project =
                            projectService.getProjectById(projectId)
                                    .orElse(null);

                    if (project != null) {
                        List<com.example.demo.model.Task> projectTasks =
                                taskService.getTasksByProject(project);
                        taskCount += projectTasks.size();
                    }
                }
            }

            // ✅ Użyj nowej metody toDtoWithStats
            TeamDto teamDto = teamMapper.toDtoWithStats(team, currentUser, projectCount, taskCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team retrieved successfully");
            response.put("data", teamDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve team: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // POST /api/v1/teams - Create new team
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTeam(@RequestBody CreateTeamRequest request) {

        try {
            User creator = getTestUser();

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            // Create team
            Team team = teamMapper.toEntity(request, creator);
            Team savedTeam = teamService.createTeam(team, creator);

            TeamDto teamDto = teamMapper.toDtoComplete(savedTeam, creator);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team created successfully");
            response.put("data", teamDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to create team: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/v1/teams/{id} - Update team
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTeam(
            @PathVariable Long id,
            @RequestBody UpdateTeamRequest request) {

        try {
            User currentUser = getTestUser();

            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + id + " not found"));

            // Check if user can edit (creator or super admin)
            if (!canModifyTeam(team, currentUser)) {
                return createErrorResponse("Only team creator or super admin can edit this team", HttpStatus.FORBIDDEN);
            }

            // Update team
            teamMapper.updateEntity(team, request);
            Team updatedTeam = teamService.updateTeam(team);

            TeamDto teamDto = teamMapper.toDtoComplete(updatedTeam, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team updated successfully");
            response.put("data", teamDto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to update team: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/teams/{id} - Delete team
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTeam(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + id + " not found"));

            // Check if user can delete (creator or super admin)
            if (!canModifyTeam(team, currentUser)) {
                return createErrorResponse("Only team creator or super admin can delete this team", HttpStatus.FORBIDDEN);
            }

            teamService.deleteTeam(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to delete team: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/teams/{id}/members - Get team members
    @GetMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> getTeamMembers(@PathVariable Long id) {

        try {
            User currentUser = getTestUser();

            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + id + " not found"));

            checkTeamAccess(team, currentUser);

            List<User> members = teamService.getTeamMembers(team);
            List<UserDto> memberDtos = userMapper.toDto(members);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team members retrieved successfully");
            response.put("data", memberDtos);
            response.put("teamId", id);
            response.put("teamName", team.getName());
            response.put("totalMembers", members.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to retrieve team members: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/teams/{id}/members - Add member to team
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMemberToTeam(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        try {
            User currentUser = getTestUser();

            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + id + " not found"));

            // Check if user can modify team
            if (!canModifyTeam(team, currentUser)) {
                return createErrorResponse("Only team creator or super admin can add members", HttpStatus.FORBIDDEN);
            }

            Long userId = Long.valueOf(request.get("userId").toString());
            User userToAdd = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Check if user is already a member
            if (team.getMembers().contains(userToAdd)) {
                return createErrorResponse("User is already a member of this team", HttpStatus.BAD_REQUEST);
            }

            teamService.addUserToTeam(team, userToAdd);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member added to team successfully");
            response.put("teamId", id);
            response.put("userId", userId);
            response.put("username", userToAdd.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to add member: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/teams/{teamId}/members/{userId} - Remove member from team
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMemberFromTeam(
            @PathVariable Long teamId,
            @PathVariable Long userId) {

        try {
            User currentUser = getTestUser();

            Team team = teamService.getTeamById(teamId)
                    .orElseThrow(() -> new RuntimeException("Team with ID " + teamId + " not found"));

            // Check if user can modify team
            if (!canModifyTeam(team, currentUser)) {
                return createErrorResponse("Only team creator or super admin can remove members", HttpStatus.FORBIDDEN);
            }

            User userToRemove = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found"));

            // Check if user is a member
            if (!team.getMembers().contains(userToRemove)) {
                return createErrorResponse("User is not a member of this team", HttpStatus.BAD_REQUEST);
            }

            teamService.removeUserFromTeam(team, userToRemove);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member removed from team successfully");
            response.put("teamId", teamId);
            response.put("userId", userId);

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

    // Helper methods
    private User getTestUser() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            throw new RuntimeException("No users found in database");
        }
        return users.get(0);
    }

    private void checkTeamAccess(Team team, User user) {
        // Super admin has access to all teams
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return;
        }

        // Check if user is a member or creator
        boolean isMember = team.getMembers() != null && team.getMembers().contains(user);
        boolean isCreator = team.getCreatedBy() != null && team.getCreatedBy().equals(user);

        if (!isMember && !isCreator) {
            throw new IllegalArgumentException("Access denied to team with ID " + team.getId());
        }
    }

    private boolean canModifyTeam(Team team, User user) {
        // Super admin can modify any team
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return true;
        }

        // Creator can modify their team
        return team.getCreatedBy() != null && team.getCreatedBy().equals(user);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}