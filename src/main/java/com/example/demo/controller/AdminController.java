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

    // Sprawdź czy użytkownik jest Super Administratorem
    private void checkSuperAdminAccess(UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        if (currentUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new RuntimeException("Brak uprawnień administratora");
        }
    }

    // Panel główny administratora
    @GetMapping
    public String adminPanel(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        // Statystyki systemu
        long totalUsers = userService.getTotalUsersCount();
        long activeUsers = userService.getActiveUsersCount();
        List<Project> projects = projectService.getAllProjects();
        List<Team> teams = teamService.getAllTeams();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("totalTeams", teams.size());

        return "admin-dashboard";
    }

    // Zarządzanie użytkownikami
    @GetMapping("/users")
    public String manageUsers(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        checkSuperAdminAccess(userDetails);

        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        List<User> users = userService.getAllUsers();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);

        return "admin-users";
    }

    // Tworzenie użytkownika
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

    // NOWA METODA - Usuwanie projektów
    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            Optional<Project> projectOpt = projectService.getProjectById(id);

            if (projectOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Projekt nie został znaleziony");
                return "redirect:/admin/projects";
            }

            Project project = projectOpt.get();
            String projectName = project.getName();

            // Usuń projekt (kaskadowo usuwa zadania, członków, etc.)
            projectService.deleteProjectByAdmin(id);

            redirectAttributes.addFlashAttribute("success",
                    "Pomyślnie usunięto projekt: " + projectName + " wraz z wszystkimi powiązanymi danymi");

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania projektu " + id + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas usuwania projektu: " + e.getMessage());
        }

        return "redirect:/admin/projects";
    }
    // Tworzenie zespołu
    @PostMapping("/teams/create")
    public String createTeam(@RequestParam String name,
                             @RequestParam(required = false) String description,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            // Sprawdź czy zespół o takiej nazwie już istnieje
            if (teamService.existsByName(name)) {
                redirectAttributes.addFlashAttribute("error", "Zespół o nazwie '" + name + "' już istnieje");
                return "redirect:/admin/teams";
            }

            Team team = teamService.createTeam(name, description);
            redirectAttributes.addFlashAttribute("success", "Pomyślnie utworzono zespół: " + name);

        } catch (Exception e) {
            System.err.println("Błąd tworzenia zespołu: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Błąd tworzenia zespołu: " + e.getMessage());
        }

        return "redirect:/admin/teams";
    }

    // Usuwanie zespołu
    @PostMapping("/teams/{id}/delete")
    public String deleteTeam(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            checkSuperAdminAccess(userDetails);

            Optional<Team> teamOpt = teamService.getTeamById(id);

            if (teamOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Zespół nie został znaleziony");
                return "redirect:/admin/teams";
            }

            Team team = teamOpt.get();
            String teamName = team.getName();

            // Usuń zespół (kaskadowo usuwa powiązania z użytkownikami)
            teamService.deleteTeamByAdmin(id);

            redirectAttributes.addFlashAttribute("success",
                    "Pomyślnie usunięto zespół: " + teamName + " wraz z wszystkimi powiązaniami");

        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania zespołu " + id + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas usuwania zespołu: " + e.getMessage());
        }

        return "redirect:/admin/teams";
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