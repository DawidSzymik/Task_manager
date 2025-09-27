// src/main/java/com/example/demo/api/dto/request/UpdateUserRequest.java
package com.example.demo.api.dto.request;

import com.example.demo.model.SystemRole;

import javax.validation.constraints.*;

public class UpdateUserRequest {

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    private SystemRole systemRole;

    private Boolean active;

    // Constructors
    public UpdateUserRequest() {}

    public UpdateUserRequest(String email, String fullName, SystemRole systemRole, Boolean active) {
        this.email = email;
        this.fullName = fullName;
        this.systemRole = systemRole;
        this.active = active;
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public SystemRole getSystemRole() { return systemRole; }
    public void setSystemRole(SystemRole systemRole) { this.systemRole = systemRole; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}