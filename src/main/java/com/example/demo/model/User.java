// src/main/java/com/example/demo/model/User.java - Z AVATAREM
package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"password", "teams", "tasks", "hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private SystemRole systemRole = SystemRole.USER;

    private String email;
    private String fullName;

    // ✅ NOWE POLE - Avatar użytkownika
    @Lob
    @Column(name = "avatar", columnDefinition = "LONGBLOB")
    private byte[] avatar;

    @Column(name = "avatar_content_type")
    private String avatarContentType;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;
    private boolean isActive = true;

    @ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
    private Set<Team> teams = new HashSet<>();

    @ManyToMany(mappedBy = "assignedUsers", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

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

    public void clearAllRelations() {
        if (tasks != null) {
            tasks.clear();
        }
        if (teams != null) {
            teams.clear();
        }
    }

    public boolean isSuperAdmin() {
        return SystemRole.SUPER_ADMIN.equals(this.systemRole);
    }

    public boolean isRegularUser() {
        return SystemRole.USER.equals(this.systemRole);
    }

    // ✅ NOWE - Helper do sprawdzania czy ma avatar
    public boolean hasAvatar() {
        return avatar != null && avatar.length > 0;
    }

    // Getters and setters
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

    // ✅ NOWE - Avatar getters/setters
    public byte[] getAvatar() { return avatar; }
    public void setAvatar(byte[] avatar) { this.avatar = avatar; }

    public String getAvatarContentType() { return avatarContentType; }
    public void setAvatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; }

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
}