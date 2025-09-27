// src/main/java/com/example/demo/api/dto/response/TaskDto.java
package com.example.demo.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDto {

    private Long id;
    private String title;
    private String description;
    private String status; // NEW, IN_PROGRESS, COMPLETED, CANCELLED
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    // Related entities
    private UserDto assignedTo; // Single assignee
    private List<UserDto> assignedUsers; // Multiple assignees
    private ProjectDto project;
    private UserDto createdBy;

    // Stats
    private int commentCount;
    private int fileCount;
    private boolean hasDeadlinePassed;
    private int daysUntilDeadline;

    // Constructors
    public TaskDto() {}

    public TaskDto(Long id, String title, String status, String priority) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.priority = priority;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public UserDto getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UserDto assignedTo) { this.assignedTo = assignedTo; }

    public List<UserDto> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(List<UserDto> assignedUsers) { this.assignedUsers = assignedUsers; }

    public ProjectDto getProject() { return project; }
    public void setProject(ProjectDto project) { this.project = project; }

    public UserDto getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserDto createdBy) { this.createdBy = createdBy; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getFileCount() { return fileCount; }
    public void setFileCount(int fileCount) { this.fileCount = fileCount; }

    public boolean isHasDeadlinePassed() { return hasDeadlinePassed; }
    public void setHasDeadlinePassed(boolean hasDeadlinePassed) { this.hasDeadlinePassed = hasDeadlinePassed; }

    public int getDaysUntilDeadline() { return daysUntilDeadline; }
    public void setDaysUntilDeadline(int daysUntilDeadline) { this.daysUntilDeadline = daysUntilDeadline; }
}