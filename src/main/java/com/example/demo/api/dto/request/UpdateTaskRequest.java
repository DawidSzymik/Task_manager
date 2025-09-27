// src/main/java/com/example/demo/api/dto/request/UpdateTaskRequest.java
package com.example.demo.api.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public class UpdateTaskRequest {

    private String title;
    private String description;
    private String status; // NEW, IN_PROGRESS, COMPLETED, CANCELLED
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private LocalDateTime deadline;
    private Long assignedToId; // Single assignee
    private List<Long> assignedUserIds; // Multiple assignees

    // Constructors
    public UpdateTaskRequest() {}

    public UpdateTaskRequest(String title, String description, String status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // Validation methods
    public String getValidationError() {
        if (title != null && title.trim().isEmpty()) {
            return "Task title cannot be empty";
        }
        if (title != null && title.length() > 200) {
            return "Task title cannot exceed 200 characters";
        }
        if (description != null && description.length() > 2000) {
            return "Task description cannot exceed 2000 characters";
        }
        if (status != null && !isValidStatus(status)) {
            return "Status must be NEW, IN_PROGRESS, COMPLETED, or CANCELLED";
        }
        if (priority != null && !isValidPriority(priority)) {
            return "Priority must be LOW, MEDIUM, HIGH, or URGENT";
        }
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            return "Deadline cannot be in the past";
        }
        return null;
    }

    private boolean isValidStatus(String status) {
        return "NEW".equals(status) || "IN_PROGRESS".equals(status) ||
                "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private boolean isValidPriority(String priority) {
        return "LOW".equals(priority) || "MEDIUM".equals(priority) ||
                "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    public boolean hasUpdates() {
        return title != null || description != null || status != null ||
                priority != null || deadline != null || assignedToId != null ||
                assignedUserIds != null;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public List<Long> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<Long> assignedUserIds) { this.assignedUserIds = assignedUserIds; }
}