package com.example.demo.controller;

import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.service.TeamService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/teams")
public class TeamViewController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    // Widok listy zespołów
    @GetMapping
    public String listTeams(Model model) {
        List<Team> teams = teamService.getAllTeams();
        model.addAttribute("teams", teams);
        return "teams";
    }

    // Formularz tworzenia zespołu
    @PostMapping("/create")
    public String createTeam(@RequestParam String name) {
        teamService.createTeam(name);
        return "redirect:/teams";
    }

    // Formularz dodawania użytkownika do zespołu
    @GetMapping("/{teamId}")
    public String viewTeam(@PathVariable Long teamId, Model model) {
        Team team = teamService.getTeamById(teamId).orElse(null);
        List<User> users = userService.getAllUsers();
        model.addAttribute("team", team);
        model.addAttribute("users", users);
        return "team-details";
    }

    // Obsługa dodawania użytkownika do zespołu
    @PostMapping("/{teamId}/addUser")
    public String addUserToTeam(@PathVariable Long teamId, @RequestParam Long userId) {
        teamService.addUserToTeam(teamId, userId);
        return "redirect:/teams/" + teamId;
    }

    // Widok użytkownika i jego zespołów
    @GetMapping("/user/{userId}")
    public String userTeams(@PathVariable Long userId, Model model) {
        User user = userService.getUserById(userId).orElse(null);
        model.addAttribute("user", user);
        return "user-teams";
    }
}
