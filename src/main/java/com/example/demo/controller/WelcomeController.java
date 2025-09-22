// src/main/java/com/example/demo/controller/WelcomeController.java - ROZSZERZONY
package com.example.demo.controller;

import com.example.demo.model.ProjectMember;
import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.service.ProjectMemberService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class WelcomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberService memberService;

    @GetMapping("/welcome")
    public String welcome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            throw new RuntimeException("Błąd: Brak zalogowanego użytkownika");
        }

        User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Set<Team> teams = user.getTeams();

        // Pobierz projekty przez ProjectMemberService
        List<ProjectMember> userMemberships = memberService.getUserProjects(user);
        List<String> projectNames = userMemberships.stream()
                .map(member -> member.getProject().getName())
                .collect(Collectors.toList());

        System.out.println("Zespoły użytkownika: " + teams);
        System.out.println("Projekty użytkownika: " + projectNames);

        model.addAttribute("username", user.getUsername());
        model.addAttribute("teams", teams);
        model.addAttribute("projects", projectNames);

        // NOWE - Dodaj informacje o użytkowniku dla template'a
        model.addAttribute("currentUser", user);

        return "welcome";
    }
}