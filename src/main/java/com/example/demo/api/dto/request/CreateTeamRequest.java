// src/main/java/com/example/demo/api/dto/request/CreateTeamRequest.java
package com.example.demo.api.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 100, message = "Team name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    public CreateTeamRequest() {}

    public CreateTeamRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Validation helper
    public String getValidationError() {
        if (name == null || name.trim().isEmpty()) {
            return "Team name is required";
        }
        if (name.length() < 3 || name.length() > 100) {
            return "Team name must be between 3 and 100 characters";
        }
        if (description != null && description.length() > 500) {
            return "Description cannot exceed 500 characters";
        }
        return null;
    }

    @Override
    public String toString() {
        return "CreateTeamRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}