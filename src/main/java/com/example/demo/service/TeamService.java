// src/main/java/com/example/demo/service/TeamService.java
package com.example.demo.service;

import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.repository.TeamRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    // Pobierz wszystkie zespoły
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    // Pobierz zespół po ID
    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    // Pobierz zespoły użytkownika
    public List<Team> getTeamsByUser(User user) {
        return teamRepository.findByMembersContaining(user);
    }

    // Utwórz zespół - dla REST API (zwraca Team)
    @Transactional
    public Team createTeam(Team team, User creator) {
        team.setCreatedAt(LocalDateTime.now());
        team.setCreatedBy(creator);

        if (team.getMembers() == null) {
            team.setMembers(new ArrayList<>());
        }
        if (!team.getMembers().contains(creator)) {
            team.getMembers().add(creator);
        }

        return teamRepository.save(team);
    }

    // Utwórz zespół - dla starych kontrolerów MVC (VOID, String params)
    @Transactional
    public void createTeam(String name, String description, User creator) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setCreatedBy(creator);
        team.setCreatedAt(LocalDateTime.now());

        if (team.getMembers() == null) {
            team.setMembers(new ArrayList<>());
        }
        team.getMembers().add(creator);

        teamRepository.save(team);
    }

    // Zaktualizuj zespół
    @Transactional
    public Team updateTeam(Team team) {
        return teamRepository.save(team);
    }

    // Usuń zespół
    @Transactional
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
        teamRepository.delete(team);
    }

    // Usuń zespół przez admina
    @Transactional
    public void deleteTeamByAdmin(Long teamId) {
        deleteTeam(teamId);
    }

    // Pobierz członków zespołu
    public List<User> getTeamMembers(Team team) {
        if (team == null || team.getMembers() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(team.getMembers());
    }

    // Alias
    public List<User> getUsers(Team team) {
        return getTeamMembers(team);
    }

    // Dodaj użytkownika - zwraca Team (dla REST API)
    @Transactional
    public Team addUserToTeam(Team team, User user) {
        if (!team.getMembers().contains(user)) {
            team.getMembers().add(user);
            return teamRepository.save(team);
        }
        return team;
    }

    // Dodaj użytkownika - Long, Long (dla starych kontrolerów MVC)
    @Transactional
    public void addUserToTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!team.getMembers().contains(user)) {
            team.getMembers().add(user);
            teamRepository.save(team);
        }
    }

    // Usuń użytkownika
    @Transactional
    public Team removeUserFromTeam(Team team, User user) {
        team.getMembers().remove(user);
        return teamRepository.save(team);
    }

    // Sprawdź czy użytkownik jest członkiem
    public boolean isUserInTeam(Team team, User user) {
        return team.getMembers() != null && team.getMembers().contains(user);
    }

    // Policz członków
    public int getMemberCount(Team team) {
        return team.getMembers() != null ? team.getMembers().size() : 0;
    }

    // Zespoły utworzone przez użytkownika
    public List<Team> getTeamsCreatedByUser(User user) {
        return teamRepository.findByCreatedBy(user);
    }

    // Znajdź po nazwie
    public Optional<Team> findTeamByName(String name) {
        return teamRepository.findByName(name);
    }

    // Sprawdź czy istnieje
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }
}