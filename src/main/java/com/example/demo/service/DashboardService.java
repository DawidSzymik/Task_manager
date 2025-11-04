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
import java.util.Set;
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
    private TaskService taskService;

    @Autowired
    private UserMapper userMapper;

    // Ogólne statystyki systemu (dla admina)
    public StatsDto getSystemStats() {
        StatsDto stats = new StatsDto();

        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByIsActiveTrue());
        stats.setTotalProjects(projectRepository.count());
        stats.setTotalTeams(teamRepository.count());
        stats.setTotalTasks(taskRepository.count());
        stats.setTasksNew(taskRepository.countByStatus("NEW"));
        stats.setTasksInProgress(taskRepository.countByStatus("IN_PROGRESS"));
        stats.setTasksCompleted(taskRepository.countByStatus("COMPLETED"));
        stats.setTasksCancelled(taskRepository.countByStatus("CANCELLED"));
        stats.setTotalComments(commentRepository.count());
        stats.setTotalFiles(fileRepository.count());

        long totalTasks = stats.getTotalTasks();
        if (totalTasks > 0) {
            double completionRate = (stats.getTasksCompleted() * 100.0) / totalTasks;
            stats.setTaskCompletionRate(completionRate);
        }

        stats.setOverdueTasks(countOverdueTasks());
        stats.setTasksCompletedThisWeek(countTasksCompletedInPeriod(7));
        stats.setTasksCompletedThisMonth(countTasksCompletedInPeriod(30));

        return stats;
    }

    // Statystyki użytkownika - DOKŁADNIE TA SAMA LOGIKA CO W TaskApiController!
    public StatsDto getUserStats(User user) {
        System.out.println("\n========================================");
        System.out.println("🔍 DASHBOARD getUserStats dla: " + user.getUsername());

        StatsDto stats = new StatsDto();

        // KROK 1: Zadania przypisane - używamy DOKŁADNIE tej samej metody co TaskApiController
        // TaskApiController robi: tasks = taskService.getTasksByAssignedUser(currentUser);
        List<Task> assignedTasks = taskService.getTasksByAssignedUser(user);
        System.out.println("📋 Zadania przypisane (z taskService.getTasksByAssignedUser): " + assignedTasks.size());

        assignedTasks.forEach(task -> System.out.println("  - [" + task.getId() + "] " + task.getTitle() +
                " | Status: " + task.getStatus() +
                " | Projekt: " + (task.getProject() != null ? task.getProject().getName() : "BRAK")));

        stats.setUserTasksAssigned((long) assignedTasks.size());

        // KROK 2: Projekty - tylko unikalne projekty z przypisanych zadań
        Set<Project> projectsWithAssignedTasks = assignedTasks.stream()
                .map(Task::getProject)
                .filter(project -> project != null)
                .collect(Collectors.toSet());

        System.out.println("📁 Projekty z przypisanymi zadaniami: " + projectsWithAssignedTasks.size());
        projectsWithAssignedTasks.forEach(p -> System.out.println("  - [" + p.getId() + "] " + p.getName()));

        stats.setUserProjects((long) projectsWithAssignedTasks.size());

        // KROK 3: Zespoły użytkownika
        List<Team> userTeams = teamRepository.findByMembersContaining(user);
        System.out.println("👥 Zespoły użytkownika: " + userTeams.size());
        stats.setUserTeams((long) userTeams.size());

        // KROK 4: Zadania utworzone przez użytkownika
        List<Task> createdTasks = taskRepository.findByCreatedBy(user);
        System.out.println("✏️ Zadania utworzone: " + createdTasks.size());
        stats.setUserTasksCreated((long) createdTasks.size());

        // KROK 5: Komentarze
        List<Comment> userComments = commentRepository.findByAuthor(user);
        System.out.println("💬 Komentarze: " + userComments.size());
        stats.setUserComments((long) userComments.size());

        // KROK 6: Pliki
        List<UploadedFile> userFiles = fileRepository.findByUploadedBy(user);
        System.out.println("📎 Pliki: " + userFiles.size());
        stats.setUserFilesUploaded((long) userFiles.size());

        // KROK 7: Completion rate
        long completedTasks = assignedTasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) || "DONE".equals(task.getStatus()))
                .count();

        System.out.println("✅ Ukończone: " + completedTasks + " / " + assignedTasks.size());

        if (!assignedTasks.isEmpty()) {
            double completionRate = (completedTasks * 100.0) / assignedTasks.size();
            stats.setTaskCompletionRate(completionRate);
            System.out.println("📊 Completion rate: " + String.format("%.1f%%", completionRate));
        }

        // KROK 8: Przeterminowane
        long overdueCount = countOverdueTasksForUser(user);
        stats.setOverdueTasks(overdueCount);
        System.out.println("⚠️ Przeterminowane: " + overdueCount);

        System.out.println("========================================\n");

        return stats;
    }

    public List<ActivityDto> getRecentActivity(int limit) {
        List<ActivityDto> activities = new ArrayList<>();

        List<Task> recentTasks = taskRepository.findAll().stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Task task : recentTasks) {
            ActivityDto activity = new ActivityDto();
            activity.setType("TASK_CREATED");
            activity.setDescription("Zadanie utworzone: " + task.getTitle());
            activity.setTimestamp(task.getCreatedAt());
            activity.setUser(userMapper.toDto(task.getCreatedBy()));
            activities.add(activity);
        }

        activities.sort(Comparator.comparing(ActivityDto::getTimestamp).reversed());
        return activities.stream().limit(limit).collect(Collectors.toList());
    }

    public List<ActivityDto> getUserRecentActivity(User user, int limit) {
        List<ActivityDto> activities = new ArrayList<>();

        List<Task> userTasks = taskRepository.findByCreatedBy(user).stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Task task : userTasks) {
            ActivityDto activity = new ActivityDto();
            activity.setType("TASK_CREATED");
            activity.setDescription("Utworzyłeś zadanie: " + task.getTitle());
            activity.setTimestamp(task.getCreatedAt());
            activity.setUser(userMapper.toDto(user));
            activities.add(activity);
        }

        List<Comment> userComments = commentRepository.findByAuthor(user).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Comment comment : userComments) {
            ActivityDto activity = new ActivityDto();
            activity.setType("COMMENT_ADDED");
            activity.setDescription("Dodałeś komentarz do zadania: " + comment.getTask().getTitle());
            activity.setTimestamp(comment.getCreatedAt());
            activity.setUser(userMapper.toDto(user));
            activities.add(activity);
        }

        activities.sort(Comparator.comparing(ActivityDto::getTimestamp).reversed());
        return activities.stream().limit(limit).collect(Collectors.toList());
    }

    private long countOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findAll().stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(now))
                .filter(task -> !"COMPLETED".equals(task.getStatus()))
                .filter(task -> !"DONE".equals(task.getStatus()))
                .filter(task -> !"CANCELLED".equals(task.getStatus()))
                .count();
    }

    private long countOverdueTasksForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        // Używamy tej samej metody co w getUserStats
        return taskService.getTasksByAssignedUser(user).stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(now))
                .filter(task -> !"COMPLETED".equals(task.getStatus()))
                .filter(task -> !"DONE".equals(task.getStatus()))
                .filter(task -> !"CANCELLED".equals(task.getStatus()))
                .count();
    }

    private long countTasksCompletedInPeriod(int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return taskRepository.findAll().stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) || "DONE".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .filter(task -> task.getCompletedAt().isAfter(startDate))
                .count();
    }
}