// src/main/java/com/example/demo/api/dto/response/UserDto.java
package com.example.demo.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String systemRole;
    private boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLogin;

    private List<String> teamNames;
    private int projectCount;

    // Constructors
    public UserDto() {}

    public UserDto(Long id, String username, String email, String systemRole, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.systemRole = systemRole;
        this.active = active;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public List<String> getTeamNames() { return teamNames; }
    public void setTeamNames(List<String> teamNames) { this.teamNames = teamNames; }

    public int getProjectCount() { return projectCount; }
    public void setProjectCount(int projectCount) { this.projectCount = projectCount; }
}