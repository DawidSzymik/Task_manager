// src/main/java/com/example/demo/api/dto/request/CreateTaskRequest.java
package com.example.demo.api.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public class CreateTaskRequest {

    private String title;
    private String description;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private LocalDateTime deadline;
    private Long projectId;
    private Long assignedToId; // Single assignee (optional)
    private List<Long> assignedUserIds; // Multiple assignees (optional)

    // Constructors
    public CreateTaskRequest() {}

    public CreateTaskRequest(String title, String description, Long projectId) {
        this.title = title;
        this.description = description;
        this.projectId = projectId;
    }

    // Validation methods
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
                projectId != null;
    }

    public String getValidationError() {
        if (title == null || title.trim().isEmpty()) {
            return "Task title is required";
        }
        if (title.length() > 200) {
            return "Task title cannot exceed 200 characters";
        }
        if (projectId == null) {
            return "Project ID is required";
        }
        if (description != null && description.length() > 2000) {
            return "Task description cannot exceed 2000 characters";
        }
        if (priority != null && !isValidPriority(priority)) {
            return "Priority must be LOW, MEDIUM, HIGH, or URGENT";
        }
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            return "Deadline cannot be in the past";
        }
        return null;
    }

    private boolean isValidPriority(String priority) {
        return "LOW".equals(priority) || "MEDIUM".equals(priority) ||
                "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public List<Long> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<Long> assignedUserIds) { this.assignedUserIds = assignedUserIds; }
}