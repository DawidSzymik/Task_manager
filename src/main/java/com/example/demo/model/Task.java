// src/main/java/com/example/demo/model/Task.java
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Set;
import java.util.HashSet;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private String status = "TODO"; // domyślnie TODO

    private LocalDateTime createdAt = LocalDateTime.now();

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadline;

    // Task należy do Project
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    // DODANE - Pojedynczy przypisany użytkownik (dla TaskService.unassignUserFromAllTasks)
    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // Wielu przypisanych użytkowników (dla bardziej zaawansowanych funkcji)
    @ManyToMany
    @JoinTable(
            name = "task_users",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();
// DODAJ TE POLA I METODY DO src/main/java/com/example/demo/model/Task.java

    // 1. DODAJ BRAKUJĄCE POLA (po istniejących polach):
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

// 2. DODAJ BRAKUJĄCE GETTERY I SETTERY (po istniejących):

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    // 3. POPRAW ISTNIEJĄCY GETTER assignedTo (jeśli jest problem):

    // Konstruktory
    public Task() {}

    public Task(String title, String description, Project project) {
        this.title = title;
        this.description = description;
        this.project = project;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    // DODANE - getter i setter dla assignedTo
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    public Set<User> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(Set<User> assignedUsers) { this.assignedUsers = assignedUsers; }

    // Metody pomocnicze
    public boolean isAssignedToUser(String username) {
        if (assignedTo != null && assignedTo.getUsername().equals(username)) {
            return true;
        }
        return assignedUsers != null && assignedUsers.stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }

    public void addAssignedUser(User user) {
        if (assignedUsers == null) {
            assignedUsers = new HashSet<>();
        }
        assignedUsers.add(user);
        // Opcjonalnie ustaw też assignedTo na pierwszego użytkownika
        if (assignedTo == null) {
            assignedTo = user;
        }
    }

    public void removeAssignedUser(User user) {
        if (assignedUsers != null) {
            assignedUsers.remove(user);
        }
        if (assignedTo != null && assignedTo.equals(user)) {
            assignedTo = null;
        }
    }
}