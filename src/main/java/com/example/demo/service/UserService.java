// src/main/java/com/example/demo/service/UserService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

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
    private PasswordEncoder passwordEncoder;

    // ========== PODSTAWOWE METODY ==========

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

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ========== STATYSTYKI ==========

    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUsersCount() {
        return userRepository.countByIsActiveTrue();
    }

    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }

    public long countBySuperAdminRole() {
        return userRepository.countBySystemRole(SystemRole.SUPER_ADMIN);
    }

    // ========== POBIERANIE UŻYTKOWNIKÓW ==========

    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    public List<User> getInactiveUsers() {
        return userRepository.findByIsActiveFalse();
    }

    public List<User> getAllSuperAdmins() {
        return userRepository.findBySystemRole(SystemRole.SUPER_ADMIN);
    }

    public List<User> searchUsers(String search) {
        return userRepository.findAll().stream()
                .filter(user ->
                        user.getUsername().toLowerCase().contains(search.toLowerCase()) ||
                                (user.getEmail() != null && user.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                                (user.getFullName() != null && user.getFullName().toLowerCase().contains(search.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    // ========== METODY DLA ZESPOŁÓW ==========

    public List<User> getUsers(Team team) {
        if (team == null || team.getMembers() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(team.getMembers());
    }

    public List<Team> findByUsersContaining(User user) {
        return teamRepository.findByMembersContaining(user);
    }

    // ========== TWORZENIE UŻYTKOWNIKÓW ==========

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
    public User registerNewUser(String username, String password) {
        return createUser(username, password);
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

    // ========== AKTUALIZACJA UŻYTKOWNIKÓW ==========

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserByAdmin(Long userId, String email, String fullName, SystemRole systemRole, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        user.setEmail(email);
        user.setFullName(fullName);
        user.setSystemRole(systemRole);
        user.setActive(isActive);

        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
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

    // ========== SPRAWDZANIE UPRAWNIEŃ ==========

    public boolean isSuperAdmin(String username) {
        return getUserByUsername(username)
                .map(user -> user.getSystemRole() == SystemRole.SUPER_ADMIN)
                .orElse(false);
    }

    public boolean canUserBeDeleted(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getSystemRole() != SystemRole.SUPER_ADMIN;
    }

    // ========== USUWANIE UŻYTKOWNIKA ==========

    @Transactional
    public void deleteUserByAdmin(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Użytkownik nie został znaleziony");
            }

            User userToDelete = userOpt.get();
            String username = userToDelete.getUsername();

            System.out.println("Rozpoczęcie usuwania użytkownika: " + username + " (ID: " + userId + ")");

            // 1. Projekty utworzone przez użytkownika
            List<Project> createdProjects = projectRepository.findByCreatedBy(userToDelete);
            for (Project project : createdProjects) {
                List<ProjectMember> projectAdmins = projectMemberRepository.findByProjectAndRole(project, ProjectRole.ADMIN);
                List<ProjectMember> otherAdmins = projectAdmins.stream()
                        .filter(admin -> !admin.getUser().equals(userToDelete))
                        .toList();

                if (!otherAdmins.isEmpty()) {
                    User newCreator = otherAdmins.get(0).getUser();
                    project.setCreatedBy(newCreator);
                    projectRepository.save(project);
                } else {
                    // Usuń projekt
                    List<Task> projectTasks = taskRepository.findByProject(project);
                    for (Task task : projectTasks) {
                        commentRepository.deleteByTask(task);
                        uploadedFileRepository.deleteByTask(task);
                        task.getAssignedUsers().clear();
                        taskRepository.save(task);
                        taskRepository.delete(task);
                    }
                    projectMemberRepository.deleteByProject(project);
                    projectRepository.delete(project);
                }
            }

            // 2. Usuń z członkostw projektów
            List<ProjectMember> projectMemberships = projectMemberRepository.findByUser(userToDelete);
            projectMemberRepository.deleteAll(projectMemberships);

            // 3. Usuń z zadań (assigned_to)
            List<Task> assignedTasks = taskRepository.findByAssignedTo(userToDelete);
            for (Task task : assignedTasks) {
                task.setAssignedTo(null);
                taskRepository.save(task);
            }

            // 4. Usuń z zadań (many-to-many)
            List<Task> allTasks = taskRepository.findAll();
            for (Task task : allTasks) {
                if (task.getAssignedUsers().remove(userToDelete)) {
                    taskRepository.save(task);
                }
            }

            // 5. Usuń komentarze
            List<Comment> userComments = commentRepository.findByAuthor(userToDelete);
            commentRepository.deleteAll(userComments);

            // 6. Usuń pliki
            List<UploadedFile> userFiles = uploadedFileRepository.findByUploadedBy(userToDelete);
            uploadedFileRepository.deleteAll(userFiles);

            // 7. Usuń powiadomienia
            List<Notification> userNotifications = notificationRepository.findByUser(userToDelete);
            notificationRepository.deleteAll(userNotifications);

            // 8. Usuń z zespołów
            List<Team> userTeams = teamRepository.findByMembersContaining(userToDelete);
            for (Team team : userTeams) {
                team.getMembers().remove(userToDelete);
                teamRepository.save(team);
            }

            // 9. Wyczyść relacje użytkownika
            if (userToDelete.getTeams() != null) {
                userToDelete.getTeams().clear();
            }
            userRepository.saveAndFlush(userToDelete);

            // 10. Usuń użytkownika
            userRepository.delete(userToDelete);
            userRepository.flush();

            System.out.println("Pomyślnie usunięto użytkownika: " + username);

        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania użytkownika " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się usunąć użytkownika: " + e.getMessage(), e);
        }
    }
}