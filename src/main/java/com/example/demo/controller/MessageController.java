// src/main/java/com/example/demo/controller/MessageController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/chat")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberService projectMemberService;

    // Wyświetl czat projektu
    @GetMapping
    public String showProjectChat(@PathVariable Long projectId, Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        // Sprawdź dostęp do projektu
        Optional<ProjectMember> memberOpt = projectMemberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        List<Message> messages = messageService.getProjectMessages(project);

        model.addAttribute("project", project);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRole", memberOpt.get().getRole());

        return "project-chat";
    }

    // Wyślij wiadomość - AJAX
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@PathVariable Long projectId,
                                                           @RequestParam String content,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            Project project = projectService.getProjectById(projectId).orElseThrow();

            if (content == null || content.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Wiadomość nie może być pusta");
                return ResponseEntity.badRequest().body(response);
            }

            if (content.length() > 1000) {
                response.put("success", false);
                response.put("error", "Wiadomość jest za długa (max 1000 znaków)");
                return ResponseEntity.badRequest().body(response);
            }

            Message message = messageService.sendMessage(project, currentUser, content.trim());

            response.put("success", true);
            response.put("messageId", message.getId());
            response.put("content", message.getContent());
            response.put("author", message.getAuthorName());
            response.put("timestamp", message.getFormattedTime());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Wyszukaj wiadomości
    @GetMapping("/search")
    public String searchMessages(@PathVariable Long projectId,
                                 @RequestParam String q,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getProjectById(projectId).orElseThrow();

        // Sprawdź dostęp
        Optional<ProjectMember> memberOpt = projectMemberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        List<Message> searchResults = messageService.searchMessages(project, q);

        model.addAttribute("project", project);
        model.addAttribute("messages", searchResults);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("searchQuery", q);
        model.addAttribute("userRole", memberOpt.get().getRole());

        return "project-chat";
    }
}