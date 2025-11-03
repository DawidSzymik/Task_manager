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

    private String status = "TODO";

    private LocalDateTime createdAt = LocalDateTime.now();

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadline;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // ✅ KRYTYCZNE: EAGER fetch żeby zawsze ładować użytkowników
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "task_users",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();

    private String priority = "MEDIUM";
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // Konstruktory
    public Task() {
        this.assignedUsers = new HashSet<>(); // ✅ Zawsze inicjalizuj!
    }

    public Task(String title, String description, Project project) {
        this();
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

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    // ✅ KRYTYCZNE: Getter ZAWSZE zwraca zainicjalizowany Set
    public Set<User> getAssignedUsers() {
        if (assignedUsers == null) {
            assignedUsers = new HashSet<>();
        }
        return assignedUsers;
    }

    public void setAssignedUsers(Set<User> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    // Metody pomocnicze
    public boolean isAssignedToUser(String username) {
        if (assignedTo != null && assignedTo.getUsername().equals(username)) {
            return true;
        }
        return getAssignedUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }

    public void addAssignedUser(User user) {
        getAssignedUsers().add(user);
        if (assignedTo == null) {
            assignedTo = user;
        }
    }

    public void removeAssignedUser(User user) {
        getAssignedUsers().remove(user);
        if (assignedTo != null && assignedTo.equals(user)) {
            assignedTo = null;
        }
    }
}