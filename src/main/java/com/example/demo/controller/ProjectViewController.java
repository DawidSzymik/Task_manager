// src/main/java/com/example/demo/controller/ProjectViewController.java - POPRAWIONY
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
import java.util.Set;
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

    @Autowired
    private ProjectMemberService memberService;

    // GŁÓWNA LISTA PROJEKTÓW
    @GetMapping
    public String listProjects(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        // Pobierz członkostwa użytkownika
        List<ProjectMember> userMemberships = memberService.getUserProjects(currentUser);

        System.out.println("Liczba członkostw użytkownika: " + userMemberships.size());

        model.addAttribute("userMemberships", userMemberships);
        return "projects";
    }

    // TWORZENIE PROJEKTU
    @PostMapping("/create")
    public String createProject(@RequestParam String name,
                                @RequestParam String description,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User creator = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        // 1. Utwórz projekt
        Project project = projectService.createProject(name, description, creator);
        System.out.println("Utworzono projekt: " + project.getName() + " (ID: " + project.getId() + ")");

        // 2. Dodaj twórcę jako admina
        ProjectMember membership = memberService.addMemberToProject(project, creator, ProjectRole.ADMIN);
        System.out.println("Dodano członkostwo: " + membership.getUser().getUsername() + " jako " + membership.getRole());

        return "redirect:/projects";
    }

    // SZCZEGÓŁY PROJEKTU - POPRAWIONE
    @GetMapping("/{projectId}")
    public String viewProject(@PathVariable Long projectId, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        // Sprawdź czy użytkownik ma dostęp
        ProjectMember currentMembership = memberService.getProjectMember(project, currentUser)
                .orElseThrow(() -> new RuntimeException("Brak dostępu do projektu"));

        List<ProjectMember> members = memberService.getProjectMembers(project);
        List<Team> teams = teamService.getAllTeams();
        List<User> allUsers = userService.getAllUsers();

        // DODAJ: Lista ID członków projektu (potrzebna w template)
        Set<Long> memberUserIds = members.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        // Podziel użytkowników na tych w zespołach i bez zespołów
        List<User> usersInTeams = teams.stream()
                .flatMap(team -> team.getUsers().stream())
                .distinct()
                .collect(Collectors.toList());

        List<User> usersWithoutTeam = allUsers.stream()
                .filter(user -> user.getTeams() == null || user.getTeams().isEmpty())
                .collect(Collectors.toList());

        model.addAttribute("project", project);
        model.addAttribute("members", members);
        model.addAttribute("teams", teams);
        model.addAttribute("usersWithoutTeam", usersWithoutTeam);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("memberUserIds", memberUserIds); // KLUCZOWE - to brakowało!
        model.addAttribute("userRole", currentMembership.getRole());
        model.addAttribute("isAdmin", currentMembership.getRole() == ProjectRole.ADMIN);

        return "project-details";
    }

    // DODAWANIE UŻYTKOWNIKA
    @PostMapping("/{projectId}/addUser")
    public String addUserToProject(@PathVariable Long projectId,
                                   @RequestParam Long userId,
                                   @RequestParam ProjectRole role,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();
        User userToAdd = userService.getUserById(userId).orElseThrow();

        // Sprawdź uprawnienia
        if (!memberService.isProjectAdmin(project, currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        memberService.addMemberToProject(project, userToAdd, role);
        return "redirect:/projects/" + projectId;
    }

    // USUWANIE UŻYTKOWNIKA
    @PostMapping("/{projectId}/removeUser")
    public String removeUserFromProject(@PathVariable Long projectId,
                                        @RequestParam Long userId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();
        User userToRemove = userService.getUserById(userId).orElseThrow();

        if (!memberService.isProjectAdmin(project, currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        if (project.getCreatedBy().equals(userToRemove)) {
            throw new RuntimeException("Nie można usunąć twórcy projektu");
        }

        memberService.removeMemberFromProject(project, userToRemove);
        return "redirect:/projects/" + projectId;
    }

    // ZMIANA ROLI
    @PostMapping("/{projectId}/changeRole")
    public String changeUserRole(@PathVariable Long projectId,
                                 @RequestParam Long userId,
                                 @RequestParam ProjectRole newRole,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();
        User targetUser = userService.getUserById(userId).orElseThrow();

        if (!memberService.isProjectAdmin(project, currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        memberService.changeUserRole(project, targetUser, newRole, currentUser);
        return "redirect:/projects/" + projectId;
    }
}