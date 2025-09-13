// src/main/java/com/example/demo/controller/ProjectViewController.java - ROZSZERZONY
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

    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private TeamService teamService;
    @Autowired private ProjectMemberService memberService;
    @Autowired private TaskProposalService taskProposalService;
    @Autowired private StatusChangeRequestService statusChangeRequestService;

    // GŁÓWNA LISTA PROJEKTÓW
    @GetMapping
    public String listProjects(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        List<ProjectMember> userMemberships = memberService.getUserProjects(currentUser);

        model.addAttribute("userMemberships", userMemberships);
        return "projects";
    }

    // TWORZENIE PROJEKTU
    @PostMapping("/create")
    public String createProject(@RequestParam String name,
                                @RequestParam String description,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User creator = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        Project project = projectService.createProject(name, description, creator);
        memberService.addMemberToProject(project, creator, ProjectRole.ADMIN);

        return "redirect:/projects";
    }

    // SZCZEGÓŁY PROJEKTU
    @GetMapping("/{projectId}")
    public String viewProject(@PathVariable Long projectId, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        ProjectMember currentMembership = memberService.getProjectMember(project, currentUser)
                .orElseThrow(() -> new RuntimeException("Brak dostępu do projektu"));

        List<ProjectMember> members = memberService.getProjectMembers(project);
        List<Team> teams = teamService.getAllTeams();
        List<User> allUsers = userService.getAllUsers();

        Set<Long> memberUserIds = members.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        List<User> usersInTeams = teams.stream()
                .flatMap(team -> team.getUsers().stream())
                .distinct()
                .collect(Collectors.toList());

        List<User> usersWithoutTeam = allUsers.stream()
                .filter(user -> user.getTeams() == null || user.getTeams().isEmpty())
                .collect(Collectors.toList());

        // DODAJ PROŚBY I PROPOZYCJE DLA ADMINÓW
        if (currentMembership.getRole() == ProjectRole.ADMIN) {
            List<TaskProposal> pendingProposals = taskProposalService.getProposalsByProject(project)
                    .stream().filter(p -> p.getStatus() == ProposalStatus.PENDING).collect(Collectors.toList());

            List<StatusChangeRequest> pendingStatusRequests = statusChangeRequestService.getPendingRequestsForAdmin(currentUser)
                    .stream().filter(r -> r.getTask().getProject().equals(project)).collect(Collectors.toList());

            model.addAttribute("pendingProposals", pendingProposals);
            model.addAttribute("pendingStatusRequests", pendingStatusRequests);
        }

        model.addAttribute("project", project);
        model.addAttribute("members", members);
        model.addAttribute("teams", teams);
        model.addAttribute("usersWithoutTeam", usersWithoutTeam);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("memberUserIds", memberUserIds);
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

    // NOWE - ZATWIERDZANIE PROPOZYCJI ZADANIA
    @PostMapping("/{projectId}/proposals/{proposalId}/approve")
    public String approveProposal(@PathVariable Long projectId, @PathVariable Long proposalId,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        if (!memberService.isProjectAdmin(project, currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        taskProposalService.approveProposal(proposalId, currentUser);
        return "redirect:/projects/" + projectId;
    }

    // NOWE - ODRZUCANIE PROPOZYCJI ZADANIA
    @PostMapping("/{projectId}/proposals/{proposalId}/reject")
    public String rejectProposal(@PathVariable Long projectId, @PathVariable Long proposalId,
                                 @RequestParam String reason,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        if (!memberService.isProjectAdmin(project, currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        taskProposalService.rejectProposal(proposalId, currentUser, reason);
        return "redirect:/projects/" + projectId;
    }
}