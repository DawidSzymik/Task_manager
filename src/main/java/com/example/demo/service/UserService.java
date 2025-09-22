// src/main/java/com/example/demo/service/UserService.java
package com.example.demo.service;

import com.example.demo.model.SystemRole;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private BCryptPasswordEncoder passwordEncoder;

    // WAŻNE: Te serwisy mogą nie istnieć na początku, dlatego required = false
    @Autowired(required = false)
    private ProjectMemberService projectMemberService;

    @Autowired(required = false)
    private TeamMemberService teamMemberService;

    @Autowired(required = false)
    private TaskService taskService;

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

    // Rejestracja nowego użytkownika (normalna rejestracja)
    public User registerNewUser(String username, String password) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
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

    // Usuwanie użytkownika przez super admina - NAPRAWIONE
    // W UserService.java zastąp metodę deleteUserByAdmin:

    @Transactional
    public void deleteUserByAdmin(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Użytkownik nie został znalezony");
            }

            User userToDelete = userOpt.get();
            String username = userToDelete.getUsername();

            System.out.println("Rozpoczęcie usuwania użytkownika: " + username + " (ID: " + userId + ")");

            // 1. NAJPIERW: Manualne usunięcie z tabeli task_users
            System.out.println("Ręczne usuwanie z tabeli task_users...");

            // Pobierz wszystkie zadania przypisane do użytkownika
            if (taskService != null) {
                List<Task> userTasks = taskService.findByAssignedTo(userToDelete);
                for (Task task : userTasks) {
                    // Usuń użytkownika z zadania
                    task.getAssignedUsers().remove(userToDelete);
                    task.setAssignedTo(null);
                    taskService.saveTask(task);
                }
                System.out.println("Odpisano użytkownika od " + userTasks.size() + " zadań");
            }

            // 2. Wyczyść relacje po stronie użytkownika
            System.out.println("Czyszczenie relacji użytkownika...");

            // Refresh użytkownika żeby mieć najnowsze dane
            userToDelete = userRepository.findById(userId).orElseThrow();

            // Wyczyść wszystkie kolekcje
            if (userToDelete.getTasks() != null) {
                userToDelete.getTasks().clear();
            }
            if (userToDelete.getTeams() != null) {
                userToDelete.getTeams().clear();
            }

            // Zapisz i wymusz flush
            userRepository.saveAndFlush(userToDelete);

            // 3. Usuń z projektów (to może też tworzyć wiadomości systemowe)
            if (projectMemberService != null) {
                projectMemberService.removeUserFromAllProjects(userToDelete);
            }

            // 4. Usuń z zespołów
            if (teamMemberService != null) {
                teamMemberService.removeUserFromAllTeams(userToDelete);
            }

            // 5. Ponowny flush przed usunięciem
            userRepository.flush();

            // 6. Ostateczne usunięcie użytkownika
            userRepository.delete(userToDelete);
            userRepository.flush();

            System.out.println("✅ Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania użytkownika " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się usunąć użytkownika: " + e.getMessage());
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

    // Pobranie aktywnych użytkowników - POPRAWIONE
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    // Pobranie nieaktywnych użytkowników - POPRAWIONE
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

    // Aktywa/dezaktywacja użytkownika
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

    // Liczba aktywnych użytkowników - POPRAWIONE
    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }
}