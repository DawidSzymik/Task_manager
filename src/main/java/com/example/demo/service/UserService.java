// src/main/java/com/example/demo/service/UserService.java - ROZSZERZONY
package com.example.demo.service;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ProjectMemberService;
import com.example.demo.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // NOWE - dla usuwania użytkowników
    @Autowired(required = false)
    private ProjectMemberService memberService;

    @Autowired(required = false)
    private TeamService teamService;

    // Pobranie użytkownika po ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Pobranie użytkownika po nazwie użytkownika
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Pobranie wszystkich użytkowników
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Rejestracja nowego użytkownika (normalna)
    public User registerNewUser(String username, String password) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Użytkownik już istnieje!");
        }

        User user = new User(username, passwordEncoder.encode(password), SystemRole.USER);
        return userRepository.save(user);
    }

    // NOWE - Tworzenie użytkownika przez super admina
    public User createUserByAdmin(String username, String password, String email, String fullName, SystemRole systemRole) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Użytkownik o tej nazwie już istnieje!");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setSystemRole(systemRole);
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);

        return userRepository.save(user);
    }

    // NOWE - Aktualizacja użytkownika przez super admina
    public User updateUserByAdmin(Long userId, String email, String fullName, SystemRole systemRole, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setEmail(email);
        user.setFullName(fullName);
        user.setSystemRole(systemRole);
        user.setActive(isActive);

        return userRepository.save(user);
    }

    // NOWE - Usuwanie użytkownika przez super admina
    @Transactional
    public void deleteUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        String username = user.getUsername();

        try {
            System.out.println("Rozpoczynam usuwanie użytkownika: " + username + " (ID: " + userId + ")");

            // 1. Usuń użytkownika ze wszystkich projektów
            if (memberService != null) {
                memberService.removeUserFromAllProjects(user);
            }

            // 2. Usuń użytkownika ze wszystkich zespołów
            if (teamService != null) {
                teamService.removeUserFromAllTeams(user);
            }

            // 3. Odpisz użytkownika ze wszystkich zadań
            user.getTasks().clear();
            userRepository.save(user);

            // 4. Usuń użytkownika
            userRepository.delete(user);

            System.out.println("Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania użytkownika: " + username);
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania użytkownika: " + e.getMessage(), e);
        }
    }

    // NOWE - Aktualizacja czasu ostatniego logowania
    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    // NOWE - Pobranie wszystkich super adminów
    public List<User> getAllSuperAdmins() {
        return userRepository.findBySystemRole(SystemRole.SUPER_ADMIN);
    }

    // NOWE - Sprawdzenie czy użytkownik jest super adminem
    public boolean isSuperAdmin(String username) {
        return getUserByUsername(username)
                .map(User::isSuperAdmin)
                .orElse(false);
    }

    // NOWE - Pobranie aktywnych użytkowników
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    // NOWE - Pobranie nieaktywnych użytkowników
    public List<User> getInactiveUsers() {
        return userRepository.findByIsActiveFalse();
    }
}