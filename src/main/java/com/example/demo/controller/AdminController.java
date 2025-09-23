// src/main/java/com/example/demo/controller/AdminController.java
package com.example.demo.controller;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.model.Project;
import com.example.demo.model.Team;
import com.example.demo.service.UserService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.TeamService;
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

    // Sprawdzenie uprawnień super admina
    private void checkSuperAdminAccess(UserDetails userDetails) {
        if (!userService.isSuperAdmin(userDetails.getUsername())) {
            throw new RuntimeException("Brak uprawnień administratora");
        }
    }

    // Panel główny administratora
    @GetMapping
    public String adminPanel(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getActiveUserCount();
        long totalProjects = projectService.getAllProjects().size();
        long totalTeams = teamService.getAllTeams().size();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalTeams", totalTeams);

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

    // NAPRAWIONA METODA USUWANIA UŻYTKOWNIKA
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

            if (userToDelete.getSystemRole() == SystemRole.SUPER_ADMIN) {
                redirectAttributes.addFlashAttribute("error", "Nie można usunąć Super Administratora");
                return "redirect:/admin/users";
            }

            String username = userToDelete.getUsername();

            // UŻYWA NOWEJ NAPRAWIONEJ METODY
            userService.deleteUserByAdmin(id);

            redirectAttributes.addFlashAttribute("success", "Pomyślnie usunięto użytkownika: " + username + " wraz z wszystkimi powiązanymi danymi");

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania użytkownika " + id + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania użytkownika: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
    // Edycja użytkownika
    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) String fullName,
                             @RequestParam SystemRole systemRole,
                             @RequestParam boolean isActive,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            User user = userService.updateUserByAdmin(id, email, fullName, systemRole, isActive);

            redirectAttributes.addFlashAttribute("success", "Pomyślnie zaktualizowano użytkownika: " + user.getUsername());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd aktualizacji użytkownika: " + e.getMessage());
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