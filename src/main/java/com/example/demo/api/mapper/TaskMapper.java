// src/main/java/com/example/demo/api/mapper/TaskMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.request.CreateTaskRequest;
import com.example.demo.api.dto.request.UpdateTaskRequest;
import com.example.demo.api.dto.response.TaskDto;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.service.CommentService;
import com.example.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private FileService fileService;

    // Entity to DTO (basic)
    public TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setDeadline(task.getDeadline());
        dto.setCompletedAt(task.getCompletedAt());

        // Map related entities
        if (task.getAssignedTo() != null) {
            dto.setAssignedTo(userMapper.toDto(task.getAssignedTo()));
        }

        if (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty()) {
            List<UserDto> assignedUserDtos = task.getAssignedUsers().stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
            dto.setAssignedUsers(assignedUserDtos);
        }

        if (task.getProject() != null) {
            dto.setProject(projectMapper.toDto(task.getProject()));
        }

        if (task.getCreatedBy() != null) {
            dto.setCreatedBy(userMapper.toDto(task.getCreatedBy()));
        }

        // Calculate deadline info
        calculateDeadlineInfo(dto, task);

        return dto;
    }

    // Entity to DTO (with stats)
    public TaskDto toDtoWithStats(Task task) {
        TaskDto dto = toDto(task);

        try {
            // Policz komentarze - używaj getCommentCountByTask zamiast getCommentsForTask
            long commentCount = commentService.getCommentCountByTask(task);
            dto.setCommentCount((int) commentCount);

            // Policz pliki
            long fileCount = fileService.getFileCountByTask(task);
            dto.setFileCount((int) fileCount);

        } catch (Exception e) {
            // Handle error
            dto.setCommentCount(0);
            dto.setFileCount(0);
        }

        return dto;
    }

    // Entity list to DTO list (basic)
    public List<TaskDto> toDto(List<Task> tasks) {
        return tasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Entity list to DTO list (with stats)
    public List<TaskDto> toDtoWithStats(List<Task> tasks) {
        return tasks.stream()
                .map(this::toDtoWithStats)
                .collect(Collectors.toList());
    }

    // CreateRequest to Entity
    public Task toEntity(CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setStatus("NEW"); // Default status
        task.setDeadline(request.getDeadline());
        task.setCreatedAt(LocalDateTime.now());

        return task;
    }

    // Update entity from UpdateRequest
    public void updateEntity(Task task, UpdateTaskRequest request) {
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());

            // Set completion time if task is completed
            if ("COMPLETED".equals(request.getStatus()) && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            } else if (!"COMPLETED".equals(request.getStatus())) {
                task.setCompletedAt(null);
            }
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
    }

    // Helper method to calculate deadline info
    private void calculateDeadlineInfo(TaskDto dto, Task task) {
        if (task.getDeadline() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = task.getDeadline();

            dto.setHasDeadlinePassed(deadline.isBefore(now));

            long daysUntilDeadline = ChronoUnit.DAYS.between(now, deadline);
            dto.setDaysUntilDeadline((int) daysUntilDeadline);
        } else {
            dto.setHasDeadlinePassed(false);
            dto.setDaysUntilDeadline(0);
        }
    }

    // Simple mapping for lists (no stats)
    public TaskDto toSimpleDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDeadline(task.getDeadline());

        calculateDeadlineInfo(dto, task);

        return dto;
    }
}