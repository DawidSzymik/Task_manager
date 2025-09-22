// src/main/java/com/example/demo/service/TeamService.java - DODANIE METODY DLA ADMINA
package com.example.demo.service;

import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.repository.TeamRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    public Team createTeam(String name) {
        Team team = new Team(name);
        return teamRepository.save(team);
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    public Team addUserToTeam(Long teamId, Long userId) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (teamOpt.isPresent() && userOpt.isPresent()) {
            Team team = teamOpt.get();
            User user = userOpt.get();
            team.addUser(user);
            return teamRepository.save(team);
        }
        return null;
    }

    // NOWA METODA - Usuwanie zespołu przez super admina
    @Transactional
    public void deleteTeamByAdmin(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Zespół nie istnieje"));

        String teamName = team.getName();

        try {
            System.out.println("🔴 SUPER ADMIN usuwa zespół: " + teamName + " (ID: " + teamId + ")");

            // 1. Usuń wszystkich użytkowników z zespołu (aktualizuj relacje)
            Set<User> teamUsers = team.getUsers();
            if (teamUsers != null && !teamUsers.isEmpty()) {
                System.out.println("Usuwam " + teamUsers.size() + " użytkowników z zespołu");

                // Skopiuj zestaw użytkowników do listy, żeby uniknąć ConcurrentModificationException
                List<User> usersList = List.copyOf(teamUsers);

                for (User user : usersList) {
                    try {
                        // Usuń zespół z użytkownika
                        user.getTeams().remove(team);
                        userRepository.save(user);

                        // Usuń użytkownika z zespołu
                        team.getUsers().remove(user);
                    } catch (Exception e) {
                        System.err.println("Błąd usuwania użytkownika " + user.getUsername() + " z zespołu: " + e.getMessage());
                    }
                }

                // Zapisz zmiany w zespole
                teamRepository.save(team);
            }

            // 2. Usuń sam zespół
            teamRepository.deleteById(teamId);

            System.out.println("✅ Pomyślnie usunięto zespół: " + teamName + " wraz z wszystkimi powiązanymi relacjami");

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania zespołu: " + teamName);
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania zespołu: " + e.getMessage(), e);
        }
    }

    // NOWA METODA - Usuń użytkownika ze wszystkich zespołów (dla super admina)
    @Transactional
    public void removeUserFromAllTeams(User user) {
        try {
            Set<Team> userTeams = user.getTeams();

            if (userTeams != null && !userTeams.isEmpty()) {
                System.out.println("Usuwam użytkownika " + user.getUsername() + " z " + userTeams.size() + " zespołów");

                // Skopiuj zespoły do listy
                List<Team> teamsList = List.copyOf(userTeams);

                for (Team team : teamsList) {
                    try {
                        // Usuń użytkownika z zespołu
                        team.getUsers().remove(user);
                        teamRepository.save(team);

                        // Usuń zespół z użytkownika
                        user.getTeams().remove(team);
                    } catch (Exception e) {
                        System.err.println("Błąd usuwania użytkownika z zespołu " + team.getName() + ": " + e.getMessage());
                    }
                }

                // Zapisz zmiany użytkownika
                userRepository.save(user);
                System.out.println("Pomyślnie usunięto użytkownika ze wszystkich zespołów");
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania użytkownika z zespołów: " + e.getMessage());
            e.printStackTrace();
        }
    }
}