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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private TaskService taskService;

    // Sprawdzenie uprawnień super admina
    private void checkSuperAdminAccess(UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        if (currentUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
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

        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalProjects", allProjects.size());
        model.addAttribute("totalTeams", allTeams.size());
        model.addAttribute("allUsers", allUsers);

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
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            User user = userService.createUserByAdmin(username, password, email, fullName, systemRole);

            redirectAttributes.addFlashAttribute("success", "Pomyślnie utworzono użytkownika: " + username);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd tworzenia użytkownika: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // Usuwanie użytkownika - NAPRAWIONE
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            Optional<User> userOpt = userService.getUserById(id);

            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Użytkownik nie został znaleziony");
                return "redirect:/admin/users";
            }

            User userToDelete = userOpt.get();

            // Sprawdź czy to nie Super Admin
            if (userToDelete.getSystemRole() == SystemRole.SUPER_ADMIN) {
                redirectAttributes.addFlashAttribute("error", "Nie można usunąć Super Administratora");
                return "redirect:/admin/users";
            }

            String username = userToDelete.getUsername();

            // Usuń użytkownika
            userService.deleteUserByAdmin(id);

            redirectAttributes.addFlashAttribute("success", "Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania użytkownika " + id + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas usuwania użytkownika");
        }

        return "redirect:/admin/users";
    }

    // Zarządzanie projektami
    @GetMapping("/projects")
    public String manageProjects(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);

        return "admin-projects";
    }

    // Zarządzanie zespołami
    @GetMapping("/teams")
    public String manageTeams(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        List<Team> teams = teamService.getAllTeams();
        model.addAttribute("teams", teams);

        return "admin-teams";
    }
}