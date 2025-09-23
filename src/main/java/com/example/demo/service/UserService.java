// src/main/java/com/example/demo/service/UserService.java
package com.example.demo.service;

import com.example.demo.model.SystemRole;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.ProjectMember;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.ProjectMemberRepository;
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

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    // Podstawowe operacje na użytkownikach
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // Rejestracja nowego użytkownika (normalna rejestracja)
    @Transactional
    public User registerNewUser(String username, String password) {
        if (userExists(username)) {
            throw new RuntimeException("Użytkownik już istnieje!");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSystemRole(SystemRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    // Tworzenie użytkownika przez super admina
    @Transactional
    public User createUserByAdmin(String username, String password, String email, String fullName, SystemRole systemRole) {
        if (userExists(username)) {
            throw new RuntimeException("Użytkownik o tej nazwie już istnieje");
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

    // Aktualizacja użytkownika przez super admina
    public User updateUserByAdmin(Long userId, String email, String fullName, SystemRole systemRole, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setEmail(email);
        user.setFullName(fullName);
        user.setSystemRole(systemRole);
        user.setActive(isActive);

        return userRepository.save(user);
    }

    // KOMPLETNIE NAPRAWIONA METODA USUWANIA UŻYTKOWNIKA
    @Transactional
    public void deleteUserByAdmin(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Użytkownik nie został znaleziony");
            }

            User userToDelete = userOpt.get();
            String username = userToDelete.getUsername();

            System.out.println("🗑️ Rozpoczęcie usuwania użytkownika: " + username + " (ID: " + userId + ")");

            // 1. KLUCZOWE: Usuń z projektów (project_members)
            System.out.println("🏢 Usuwanie z projektów...");
            List<ProjectMember> projectMemberships = projectMemberRepository.findByUser(userToDelete);
            for (ProjectMember membership : projectMemberships) {
                projectMemberRepository.delete(membership);
            }
            System.out.println("✅ Usunięto z " + projectMemberships.size() + " projektów");

            // 2. Usuń z relacji Many-to-Many (task_users)
            System.out.println("📋 Usuwanie z zadań (Many-to-Many)...");
            List<Task> userTasks = taskRepository.findByAssignedUsersContaining(userToDelete);
            for (Task task : userTasks) {
                task.getAssignedUsers().remove(userToDelete);
                taskRepository.save(task);
            }
            System.out.println("✅ Usunięto z " + userTasks.size() + " zadań (assignedUsers)");

            // 3. Usuń z relacji pojedynczych (assigned_to)
            List<Task> assignedTasks = taskRepository.findByAssignedTo(userToDelete);
            for (Task task : assignedTasks) {
                task.setAssignedTo(null);
                taskRepository.save(task);
            }
            System.out.println("✅ Usunięto z " + assignedTasks.size() + " zadań (assignedTo)");

            // 4. Wyczyść kolekcje po stronie użytkownika
            userToDelete.clearAllRelations();
            userRepository.saveAndFlush(userToDelete);

            // 5. Teraz można bezpiecznie usunąć użytkownika
            userRepository.delete(userToDelete);
            userRepository.flush();

            System.out.println("✅ Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania użytkownika " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się usunąć użytkownika: " + e.getMessage(), e);
        }
    }

    // Aktualizacja czasu ostatniego logowania
    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    // Sprawdzenie czy użytkownik jest super adminem
    public boolean isSuperAdmin(String username) {
        return getUserByUsername(username)
                .map(user -> user.getSystemRole() == SystemRole.SUPER_ADMIN)
                .orElse(false);
    }

    // Pobranie aktywnych użytkowników
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    // Pobranie nieaktywnych użytkowników
    public List<User> getInactiveUsers() {
        return userRepository.findByIsActiveFalse();
    }

    // Pobranie wszystkich super adminów
    public List<User> getAllSuperAdmins() {
        return userRepository.findBySystemRole(SystemRole.SUPER_ADMIN);
    }

    // Zmiana hasła użytkownika
    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Aktywacja/dezaktywacja użytkownika
    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    // Sprawdź czy użytkownik może być usunięty
    public boolean canUserBeDeleted(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // Super Admin nie może być usunięty
        return user.getSystemRole() != SystemRole.SUPER_ADMIN;
    }

    // Liczba wszystkich użytkowników
    public long getTotalUserCount() {
        return userRepository.count();
    }

    // Liczba aktywnych użytkowników
    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }
}