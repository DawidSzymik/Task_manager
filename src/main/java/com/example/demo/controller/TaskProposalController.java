// src/main/java/com/example/demo/controller/TaskProposalController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/proposals")
public class TaskProposalController {

    @Autowired
    private TaskProposalService proposalService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberService memberService;

    // Formularz propozycji zadania
    @GetMapping("/create/{projectId}")
    public String createProposalForm(@PathVariable Long projectId, Model model,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        // Sprawdź czy może proponować zadania (MEMBER lub ADMIN, ale nie VIEWER)
        ProjectRole userRole = memberService.getProjectMember(project, currentUser)
                .map(ProjectMember::getRole).orElse(null);

        if (userRole == null || userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Brak uprawnień do proponowania zadań");
        }

        model.addAttribute("project", project);
        model.addAttribute("proposal", new TaskProposal());

        return "proposal-create";
    }

    // Tworzenie propozycji
    @PostMapping("/create")
    public String createProposal(@RequestParam Long projectId,
                                 @RequestParam String title,
                                 @RequestParam String description,
                                 @RequestParam(required = false) LocalDateTime deadline,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        proposalService.createProposal(project, currentUser, title, description, deadline);

        return "redirect:/projects/" + projectId + "?proposalCreated=true";
    }

    // Zatwierdzenie propozycji (tylko admin)
    @PostMapping("/{proposalId}/approve")
    public String approveProposal(@PathVariable Long proposalId,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        TaskProposal proposal = proposalService.getProposalById(proposalId).orElseThrow();

        // Sprawdź uprawnienia
        if (!memberService.isProjectAdmin(proposal.getProject(), currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        proposalService.approveProposal(proposalId, currentUser);

        return "redirect:/projects/" + proposal.getProject().getId() + "/proposals";
    }

    // Odrzucenie propozycji (tylko admin)
    @PostMapping("/{proposalId}/reject")
    public String rejectProposal(@PathVariable Long proposalId,
                                 @RequestParam String reason,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        TaskProposal proposal = proposalService.getProposalById(proposalId).orElseThrow();

        // Sprawdź uprawnienia
        if (!memberService.isProjectAdmin(proposal.getProject(), currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        proposalService.rejectProposal(proposalId, currentUser, reason);

        return "redirect:/projects/" + proposal.getProject().getId() + "/proposals";
    }
}