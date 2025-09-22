// src/main/java/com/example/demo/model/User.java - ROZSZERZONY O ROLĘ SYSTEMOWĄ
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    // NOWE - ROLA SYSTEMOWA
    @Enumerated(EnumType.STRING)
    private SystemRole systemRole = SystemRole.USER;

    // NOWE - DANE DODATKOWE
    private String email;
    private String fullName;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;
    private boolean isActive = true;

    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)
    private Set<Team> teams = new HashSet<>();

    @ManyToMany(mappedBy = "assignedUsers", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

    // Konstruktory
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, SystemRole systemRole) {
        this.username = username;
        this.password = password;
        this.systemRole = systemRole;
    }

    // Metody pomocnicze
    public boolean isSuperAdmin() {
        return SystemRole.SUPER_ADMIN.equals(this.systemRole);
    }

    public boolean isRegularUser() {
        return SystemRole.USER.equals(this.systemRole);
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public SystemRole getSystemRole() { return systemRole; }
    public void setSystemRole(SystemRole systemRole) { this.systemRole = systemRole; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Set<Team> getTeams() { return teams; }
    public void setTeams(Set<Team> teams) { this.teams = teams; }

    public Set<Task> getTasks() { return tasks; }
    public void setTasks(Set<Task> tasks) { this.tasks = tasks; }
    public void clearAllRelations() {
        // Wyczyść relacje Many-to-Many
        if (this.tasks != null) {
            this.tasks.clear();
        }
        if (this.teams != null) {
            this.teams.clear();
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}