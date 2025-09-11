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
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Tworzenie propozycji
    @PostMapping("/create")
    public String createProposal(@RequestParam Long projectId,
                                 @RequestParam String title,
                                 @RequestParam String description,
                                 @RequestParam(required = false) String deadline,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            Project project = projectService.getProjectById(projectId).orElseThrow();

            LocalDateTime deadlineDate = null;
            if (deadline != null && !deadline.isEmpty()) {
                deadlineDate = LocalDateTime.parse(deadline);
            }

            proposalService.createProposal(project, currentUser, title, description, deadlineDate);

            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}