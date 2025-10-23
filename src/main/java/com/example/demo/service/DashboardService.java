// src/main/java/com/example/demo/service/DashboardService.java
package com.example.demo.service;

import com.example.demo.api.dto.response.ActivityDto;
import com.example.demo.api.dto.response.StatsDto;
import com.example.demo.api.mapper.UserMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UploadedFileRepository fileRepository;

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private UserMapper userMapper;

    // Ogólne statystyki systemu (dla admina)
    public StatsDto getSystemStats() {
        StatsDto stats = new StatsDto();

        // Użytkownicy
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByIsActiveTrue());

        // Projekty i zespoły
        stats.setTotalProjects(projectRepository.count());
        stats.setTotalTeams(teamRepository.count());

        // Zadania
        stats.setTotalTasks(taskRepository.count());
        stats.setTasksNew(taskRepository.countByStatus("NEW"));
        stats.setTasksInProgress(taskRepository.countByStatus("IN_PROGRESS"));
        stats.setTasksCompleted(taskRepository.countByStatus("COMPLETED"));
        stats.setTasksCancelled(taskRepository.countByStatus("CANCELLED"));

        // Komentarze i pliki
        stats.setTotalComments(commentRepository.count());
        stats.setTotalFiles(fileRepository.count());

        // Task completion rate
        long totalTasks = stats.getTotalTasks();
        if (totalTasks > 0) {
            double completionRate = (stats.getTasksCompleted() * 100.0) / totalTasks;
            stats.setTaskCompletionRate(completionRate);
        }

        // Przeterminowane zadania
        stats.setOverdueTasks(countOverdueTasks());

        // Zadania ukończone w tym tygodniu/miesiącu
        stats.setTasksCompletedThisWeek(countTasksCompletedInPeriod(7));
        stats.setTasksCompletedThisMonth(countTasksCompletedInPeriod(30));

        return stats;
    }

    // Statystyki użytkownika
    public StatsDto getUserStats(User user) {
        StatsDto stats = new StatsDto();

        // ✅ POPRAWKA: getUserProjects zwraca List<Project>, nie List<ProjectMember>
        List<Project> userProjects = projectMemberService.getUserProjects(user);
        stats.setUserProjects((long) userProjects.size());

        // Zespoły użytkownika
        List<Team> userTeams = teamRepository.findByMembersContaining(user);
        stats.setUserTeams((long) userTeams.size());

        // Zadania przypisane do użytkownika
        List<Task> assignedTasks = taskRepository.findByAssignedUsersContaining(user);
        stats.setUserTasksAssigned((long) assignedTasks.size());

        // Zadania utworzone przez użytkownika
        List<Task> createdTasks = taskRepository.findByCreatedBy(user);
        stats.setUserTasksCreated((long) createdTasks.size());

        // Komentarze użytkownika
        List<Comment> userComments = commentRepository.findByAuthor(user);
        stats.setUserComments((long) userComments.size());

        // Pliki przesłane przez użytkownika
        List<UploadedFile> userFiles = fileRepository.findByUploadedBy(user);
        stats.setUserFilesUploaded((long) userFiles.size());

        // Task completion rate użytkownika
        long completedByUser = assignedTasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .count();

        if (!assignedTasks.isEmpty()) {
            double completionRate = (completedByUser * 100.0) / assignedTasks.size();
            stats.setTaskCompletionRate(completionRate);
        }

        // Przeterminowane zadania użytkownika
        stats.setOverdueTasks(countOverdueTasksForUser(user));

        return stats;
    }

    // Ostatnia aktywność (dla wszystkich)
    public List<ActivityDto> getRecentActivity(int limit) {
        List<ActivityDto> activities = new ArrayList<>();

        // Ostatnie zadania
        List<Task> recentTasks = taskRepository.findAll().stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Task task : recentTasks) {
            ActivityDto activity = new ActivityDto();
            activity.setType("TASK_CREATED");
            activity.setTitle("Nowe zadanie: " + task.getTitle());
            activity.setTimestamp(task.getCreatedAt());
            if (task.getCreatedBy() != null) {
                activity.setUser(userMapper.toDto(task.getCreatedBy()));
            }
            activities.add(activity);
        }

        // Ostatnie komentarze
        List<Comment> recentComments = commentRepository.findAll().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Comment comment : recentComments) {
            ActivityDto activity = new ActivityDto();
            activity.setType("COMMENT_ADDED");
            activity.setTitle("Nowy komentarz w: " + comment.getTask().getTitle());
            activity.setTimestamp(comment.getCreatedAt());
            if (comment.getAuthor() != null) {
                activity.setUser(userMapper.toDto(comment.getAuthor()));
            }
            activities.add(activity);
        }

        // Sortuj wszystkie aktywności po czasie
        activities.sort(Comparator.comparing(ActivityDto::getTimestamp).reversed());

        // Zwróć tylko limit aktywności
        return activities.stream().limit(limit).collect(Collectors.toList());
    }

    // Ostatnia aktywność użytkownika
    public List<ActivityDto> getUserRecentActivity(User user, int limit) {
        List<ActivityDto> activities = new ArrayList<>();

        // Zadania użytkownika
        List<Task> userTasks = taskRepository.findByCreatedBy(user).stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Task task : userTasks) {
            ActivityDto activity = new ActivityDto();
            activity.setType("TASK_CREATED");
            activity.setTitle("Utworzyłeś zadanie: " + task.getTitle());
            activity.setTimestamp(task.getCreatedAt());
            activity.setUser(userMapper.toDto(user));
            activities.add(activity);
        }

        // Komentarze użytkownika
        List<Comment> userComments = commentRepository.findByAuthor(user).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Comment comment : userComments) {
            ActivityDto activity = new ActivityDto();
            activity.setType("COMMENT_ADDED");
            activity.setTitle("Dodałeś komentarz w: " + comment.getTask().getTitle());
            activity.setTimestamp(comment.getCreatedAt());
            activity.setUser(userMapper.toDto(user));
            activities.add(activity);
        }

        // Sortuj i zwróć
        activities.sort(Comparator.comparing(ActivityDto::getTimestamp).reversed());
        return activities.stream().limit(limit).collect(Collectors.toList());
    }

    // Helper methods
    private long countOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findAll().stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(now))
                .filter(task -> !"COMPLETED".equals(task.getStatus()))
                .filter(task -> !"CANCELLED".equals(task.getStatus()))
                .count();
    }

    private long countOverdueTasksForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        List<Task> userTasks = taskRepository.findByAssignedUsersContaining(user);
        return userTasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(now))
                .filter(task -> !"COMPLETED".equals(task.getStatus()))
                .filter(task -> !"CANCELLED".equals(task.getStatus()))
                .count();
    }

    private long countTasksCompletedInPeriod(int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return taskRepository.findAll().stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .filter(task -> task.getCompletedAt().isAfter(startDate))
                .count();
    }
}