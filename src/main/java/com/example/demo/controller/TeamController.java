package com.example.demo.controller;

import com.example.demo.model.Team;
import com.example.demo.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    // Tworzenie nowego zespołu (POST)
    @PostMapping("/create")
    public Team createTeam(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Nazwa zespołu nie może być pusta.");
        }
        return teamService.createTeam(name);
    }

    // Dodanie użytkownika do zespołu (POST)
    @PostMapping("/{teamId}/addUser/{userId}")
    public Team addUserToTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        return teamService.addUserToTeam(teamId, userId);
    }

    // Pobranie zespołu po ID (GET)
    @GetMapping("/{id}")
    public Team getTeam(@PathVariable Long id) {
        return teamService.getTeamById(id)
                .orElseThrow(() -> new RuntimeException("Zespół o ID " + id + " nie istnieje."));
    }

    // Pobranie wszystkich zespołów (GET)
    @GetMapping
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }
}
