// src/main/java/com/example/demo/controller/ProjectViewController.java - POPRAWIONY
package com.example.demo.controller;

import com.example.demo.model.Project;
import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.service.ProjectService;
import com.example.demo.service.TeamService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects")
public class ProjectViewController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    // Widok listy projektów
    @GetMapping
    public String listProjects(Model model) {
        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);
        return "projects";
    }

    // Formularz tworzenia projektu
    @PostMapping("/create")
    public String createProject(@RequestParam String name, @RequestParam String description) {
        projectService.createProject(name, description);
        return "redirect:/projects";
    }

    // Szczegóły projektu - POPRAWIONY
    @GetMapping("/{projectId}")
    public String viewProject(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        // Pobierz wszystkie zespoły
        List<Team> teams = teamService.getAllTeams();

        // Pobierz wszystkich użytkowników
        List<User> allUsers = userService.getAllUsers();

        // Znajdź użytkowników bez zespołu
        List<User> usersWithoutTeam = allUsers.stream()
                .filter(user -> user.getTeams() == null || user.getTeams().isEmpty())
                .collect(Collectors.toList());

        System.out.println("Użytkownicy bez zespołu: " + usersWithoutTeam.size()); // Debug

        model.addAttribute("project", project);
        model.addAttribute("teams", teams);
        model.addAttribute("usersWithoutTeam", usersWithoutTeam);

        return "project-details";
    }

    // Obsługa dodawania użytkownika do projektu
    @PostMapping("/{projectId}/addUser")
    public String addUserToProject(@PathVariable Long projectId, @RequestParam Long userId) {
        projectService.addUserToProject(projectId, userId);
        return "redirect:/projects/" + projectId;
    }

    // Obsługa usuwania użytkownika z projektu
    @PostMapping("/{projectId}/removeUser")
    public String removeUserFromProject(@PathVariable Long projectId, @RequestParam Long userId) {
        projectService.removeUserFromProject(projectId, userId);
        return "redirect:/projects/" + projectId;
    }
}