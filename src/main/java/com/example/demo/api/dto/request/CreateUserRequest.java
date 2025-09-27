// src/main/java/com/example/demo/api/dto/request/CreateUserRequest.java
package com.example.demo.api.dto.request;

import com.example.demo.model.SystemRole;

import javax.validation.constraints.*;
// Lub indywidualnie:
// import javax.validation.constraints.Email;
// import javax.validation.constraints.NotBlank;
// import javax.validation.constraints.NotNull;
// import javax.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotNull(message = "System role is required")
    private SystemRole systemRole;

    // Constructors
    public CreateUserRequest() {}

    public CreateUserRequest(String username, String password, String email, String fullName, SystemRole systemRole) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.systemRole = systemRole;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public SystemRole getSystemRole() { return systemRole; }
    public void setSystemRole(SystemRole systemRole) { this.systemRole = systemRole; }
}