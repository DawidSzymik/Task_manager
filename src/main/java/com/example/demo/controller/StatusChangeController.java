// src/main/java/com/example/demo/controller/StatusChangeController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/status-requests")
public class StatusChangeController {

    @Autowired
    private StatusChangeRequestService statusRequestService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberService memberService;

    // Żądanie zmiany statusu (members)
    @PostMapping("/request")
    public String requestStatusChange(@RequestParam Long taskId,
                                      @RequestParam String newStatus,
                                      @RequestParam(required = false) String returnTo,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.getTaskById(taskId).orElseThrow();

        // Sprawdź czy użytkownik może żądać zmiany statusu
        ProjectRole userRole = memberService.getProjectMember(task.getProject(), currentUser)
                .map(ProjectMember::getRole).orElse(null);

        if (userRole == null || userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Brak uprawnień do zmiany statusu");
        }

        // Admin może zmieniać bezpośrednio, member musi żądać
        if (userRole == ProjectRole.ADMIN) {
            task.setStatus(newStatus);
            taskService.saveTask(task);
        } else {
            statusRequestService.requestStatusChange(task, newStatus, currentUser);
        }

        if ("task-view".equals(returnTo)) {
            return "redirect:/tasks/view/" + taskId;
        } else {
            return "redirect:/tasks/project/" + task.getProject().getId();
        }
    }

    // Zatwierdzenie zmiany statusu (tylko admin)
    @PostMapping("/{requestId}/approve")
    public String approveStatusChange(@PathVariable Long requestId,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        StatusChangeRequest request = statusRequestService.getRequestById(requestId).orElseThrow();

        // Sprawdź uprawnienia
        if (!memberService.isProjectAdmin(request.getTask().getProject(), currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        statusRequestService.approveStatusChange(requestId, currentUser);

        return "redirect:/projects/" + request.getTask().getProject().getId() + "/status-requests";
    }

    // Odrzucenie zmiany statusu (tylko admin)
    @PostMapping("/{requestId}/reject")
    public String rejectStatusChange(@PathVariable Long requestId,
                                     @RequestParam String reason,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        StatusChangeRequest request = statusRequestService.getRequestById(requestId).orElseThrow();

        // Sprawdź uprawnienia
        if (!memberService.isProjectAdmin(request.getTask().getProject(), currentUser)) {
            throw new RuntimeException("Brak uprawnień admina");
        }

        statusRequestService.rejectStatusChange(requestId, currentUser, reason);

        return "redirect:/projects/" + request.getTask().getProject().getId() + "/status-requests";
    }
}