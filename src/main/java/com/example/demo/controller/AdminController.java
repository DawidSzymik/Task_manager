// src/main/java/com/example/demo/controller/AdminController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private TeamService teamService;
    @Autowired private ProjectMemberService memberService;
    @Autowired private TaskService taskService;
    @Autowired private NotificationService notificationService;

    // Sprawdzenie uprawnień super admina
    private void checkSuperAdminAccess(UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        if (!currentUser.isSuperAdmin()) {
            throw new RuntimeException("Brak uprawnień administratora systemu");
        }
    }

    // Panel administratora
    @GetMapping
    public String adminDashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<User> allUsers = userService.getAllUsers();
        List<Project> allProjects = projectService.getAllProjects();
        List<Team> allTeams = teamService.getAllTeams();

        // Statystyki
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long totalProjects = allProjects.size();
        long totalTeams = allTeams.size();

        // Ostatnie aktywności
        List<User> recentUsers = allUsers.stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalTeams", totalTeams);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("allProjects", allProjects);
        model.addAttribute("allTeams", allTeams);

        return "admin-dashboard";
    }

    // Zarządzanie użytkownikami
    @GetMapping("/users")
    public String manageUsers(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        return "admin-users";
    }

    // Tworzenie nowego użytkownika
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) String fullName,
                             @RequestParam SystemRole systemRole,
                             @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        User user = userService.createUserByAdmin(username, password, email, fullName, systemRole);

        System.out.println("Super Admin utworzył użytkownika: " + username + " z rolą: " + systemRole);

        return "redirect:/admin/users";
    }

    // Edycja użytkownika
    @PostMapping("/users/{userId}/edit")
    public String editUser(@PathVariable Long userId,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String fullName,
                           @RequestParam SystemRole systemRole,
                           @RequestParam boolean isActive,
                           @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        userService.updateUserByAdmin(userId, email, fullName, systemRole, isActive);

        return "redirect:/admin/users";
    }

    // Usuwanie użytkownika
    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        User currentAdmin = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (currentAdmin.getId().equals(userId)) {
            throw new RuntimeException("Nie możesz usunąć swojego własnego konta");
        }

        userService.deleteUserByAdmin(userId);

        return "redirect:/admin/users";
    }

    // Zarządzanie projektami
    @GetMapping("/projects")
    public String manageProjects(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<Project> projects = projectService.getAllProjects();

        // Dodaj informacje o członkach dla każdego projektu
        for (Project project : projects) {
            List<ProjectMember> members = memberService.getProjectMembers(project);
            project.setMembers(new java.util.HashSet<>(members));
        }

        model.addAttribute("projects", projects);

        return "admin-projects";
    }

    // Usuwanie projektu
    @PostMapping("/projects/{projectId}/delete")
    public String deleteProject(@PathVariable Long projectId,
                                @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        // Usuń projekt wraz z wszystkimi powiązanymi danymi
        projectService.deleteProjectByAdmin(projectId);

        return "redirect:/admin/projects";
    }

    // Zarządzanie zespołami
    @GetMapping("/teams")
    public String manageTeams(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<Team> teams = teamService.getAllTeams();
        model.addAttribute("teams", teams);

        return "admin-teams";
    }

    // Usuwanie zespołu
    @PostMapping("/teams/{teamId}/delete")
    public String deleteTeam(@PathVariable Long teamId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        teamService.deleteTeamByAdmin(teamId);

        return "redirect:/admin/teams";
    }

    // System logs / aktywności
    @GetMapping("/logs")
    public String viewSystemLogs(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        // Tu można dodać system logowania aktywności
        model.addAttribute("message", "System logowania będzie dodany w przyszłości");

        return "admin-logs";
    }

    // Ustawienia systemowe
    @GetMapping("/settings")
    public String systemSettings(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        // Tu można dodać różne ustawienia systemowe
        model.addAttribute("message", "Ustawienia systemowe będą dodane w przyszłości");

        return "admin-settings";
    }
}