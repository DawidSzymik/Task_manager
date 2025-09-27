// src/main/java/com/example/demo/service/UserService.java - KOMPLETNA POPRAWKA
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

            // 1. ZNAJDŹ PROJEKTY UTWORZONE PRZEZ TEGO UŻYTKOWNIKA
            System.out.println("🔍 Szukanie projektów utworzonych przez użytkownika...");
            List<Project> createdProjects = projectRepository.findByCreatedBy(userToDelete);
            System.out.println("📋 Znaleziono " + createdProjects.size() + " projektów utworzonych przez użytkownika");

            // 2. DLA KAŻDEGO PROJEKTU - PRZYPISZ NOWEGO ADMINA LUB USUŃ PROJEKT
            for (Project project : createdProjects) {
                System.out.println("🏢 Przetwarzanie projektu: " + project.getName());

                // Znajdź innych adminów w projekcie
                List<ProjectMember> projectAdmins = projectMemberRepository.findByProjectAndRole(project, ProjectRole.ADMIN);
                List<ProjectMember> otherAdmins = projectAdmins.stream()
                        .filter(admin -> !admin.getUser().equals(userToDelete))
                        .toList();

                if (!otherAdmins.isEmpty()) {
                    // Jeśli są inni adminowie - przypisz pierwszego jako nowego twórcę
                    User newCreator = otherAdmins.get(0).getUser();
                    project.setCreatedBy(newCreator);
                    projectRepository.save(project);
                    System.out.println("✅ Przypisano nowego twórcę projektu: " + newCreator.getUsername());
                } else {
                    // Jeśli nie ma innych adminów - usuń cały projekt
                    System.out.println("🗑️ Usuwanie projektu bez innych adminów: " + project.getName());

                    // Usuń wszystkie zadania z projektu (wraz z komentarzami i plikami)
                    List<Task> projectTasks = taskRepository.findByProject(project);
                    for (Task task : projectTasks) {
                        // Usuń komentarze do zadania
                        commentRepository.deleteByTask(task);
                        // Usuń pliki zadania
                        uploadedFileRepository.deleteByTask(task);
                        // Wyczyść relacje many-to-many
                        task.getAssignedUsers().clear();
                        taskRepository.save(task);
                        taskRepository.delete(task);
                    }

                    // Usuń wszystkich członków projektu
                    projectMemberRepository.deleteByProject(project);

                    // Usuń projekt
                    projectRepository.delete(project);
                    System.out.println("✅ Usunięto projekt: " + project.getName());
                }
            }

            // 3. USUŃ UŻYTKOWNIKA Z PROJEKTÓW (project_members)
            System.out.println("🏢 Usuwanie z członkostw projektów...");
            List<ProjectMember> projectMemberships = projectMemberRepository.findByUser(userToDelete);
            projectMemberRepository.deleteAll(projectMemberships);
            System.out.println("✅ Usunięto z " + projectMemberships.size() + " projektów");

            // 4. USUŃ Z ZADAŃ (assigned_to)
            System.out.println("📋 Usuwanie z przypisań zadań...");
            List<Task> assignedTasks = taskRepository.findByAssignedTo(userToDelete);
            for (Task task : assignedTasks) {
                task.setAssignedTo(null);
                taskRepository.save(task);
            }
            System.out.println("✅ Usunięto z " + assignedTasks.size() + " zadań (assignedTo)");

            // 5. USUŃ Z ZADAŃ (many-to-many assigned_users)
            System.out.println("👥 Usuwanie z relacji many-to-many zadań...");
            List<Task> allTasks = taskRepository.findAll();
            int removedFromTasks = 0;
            for (Task task : allTasks) {
                if (task.getAssignedUsers().remove(userToDelete)) {
                    taskRepository.save(task);
                    removedFromTasks++;
                }
            }
            System.out.println("✅ Usunięto z " + removedFromTasks + " zadań (many-to-many)");

            // 6. USUŃ KOMENTARZE NAPISANE PRZEZ UŻYTKOWNIKA
            System.out.println("💬 Usuwanie komentarzy...");
            List<Comment> userComments = commentRepository.findByAuthor(userToDelete);
            commentRepository.deleteAll(userComments);
            System.out.println("✅ Usunięto " + userComments.size() + " komentarzy");

            // 7. USUŃ PLIKI PRZESŁANE PRZEZ UŻYTKOWNIKA
            System.out.println("📁 Usuwanie przesłanych plików...");
            List<UploadedFile> userFiles = uploadedFileRepository.findByUploadedBy(userToDelete);
            uploadedFileRepository.deleteAll(userFiles);
            System.out.println("✅ Usunięto " + userFiles.size() + " plików");

            // 8. USUŃ POWIADOMIENIA
            System.out.println("🔔 Usuwanie powiadomień...");
            List<Notification> userNotifications = notificationRepository.findByUser(userToDelete);
            notificationRepository.deleteAll(userNotifications);
            System.out.println("✅ Usunięto " + userNotifications.size() + " powiadomień");

            // 9. USUŃ Z ZESPOŁÓW
            System.out.println("👥 Usuwanie z zespołów...");
            List<Team> userTeams = teamRepository.findByUsersContaining(userToDelete);
            for (Team team : userTeams) {
                team.getUsers().remove(userToDelete);
                teamRepository.save(team);
            }
            System.out.println("✅ Usunięto z " + userTeams.size() + " zespołów");

            // 10. WYCZYŚĆ RELACJE PO STRONIE UŻYTKOWNIKA
            userToDelete.clearAllRelations();
            userRepository.saveAndFlush(userToDelete);

            // 11. TERAZ MOŻNA BEZPIECZNIE USUNĄĆ UŻYTKOWNIKA
            userRepository.delete(userToDelete);
            userRepository.flush();

            System.out.println("✅ Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania użytkownika " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się usunąć użytkownika: " + e.getMessage(), e);
        }
    }
    // DODANE BRAKUJĄCE METODY
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public long getActiveUsersCount() {
        return userRepository.countByIsActiveTrue();
    }
    // Dodaj te metody do istniejącego UserService

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }


    public List<User> searchUsers(String search) {
        // Proste wyszukiwanie - możesz to rozszerzyć
        return userRepository.findAll().stream()
                .filter(user ->
                        user.getUsername().toLowerCase().contains(search.toLowerCase()) ||
                                (user.getEmail() != null && user.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                                (user.getFullName() != null && user.getFullName().toLowerCase().contains(search.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    public long countBySuperAdminRole() {
        return userRepository.countBySystemRole(SystemRole.SUPER_ADMIN);
    }
    // Pozostałe metody bez zmian
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Transactional
    public User createUser(String username, String password) {
        if (userExists(username)) {
            throw new RuntimeException("Użytkownik o tej nazwie już istnieje");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSystemRole(SystemRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

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

    public User updateUserByAdmin(Long userId, String email, String fullName, SystemRole systemRole, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setEmail(email);
        user.setFullName(fullName);
        user.setSystemRole(systemRole);
        user.setActive(isActive);

        return userRepository.save(user);
    }

    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    public boolean isSuperAdmin(String username) {
        return getUserByUsername(username)
                .map(user -> user.getSystemRole() == SystemRole.SUPER_ADMIN)
                .orElse(false);
    }

    public List<User> getInactiveUsers() {
        return userRepository.findByIsActiveFalse();
    }

    public List<User> getAllSuperAdmins() {
        return userRepository.findBySystemRole(SystemRole.SUPER_ADMIN);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    public boolean canUserBeDeleted(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getSystemRole() != SystemRole.SUPER_ADMIN;
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }
    public User registerNewUser(String username, String password) {
        if (userExists(username)) {
            throw new RuntimeException("Użytkownik o tej nazwie już istnieje");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSystemRole(SystemRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}