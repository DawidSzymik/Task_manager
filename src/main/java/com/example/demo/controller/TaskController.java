// src/main/java/com/example/demo/controller/TaskController.java - ZMIENIONY
package com.example.demo.controller;

import java.util.Set;
import java.util.HashSet;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.service.TaskService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.util.Optional;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskController(TaskService taskService, ProjectService projectService, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping("/create/{projectId}")
    public String createTaskForm(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));
        List<User> projectMembers = new ArrayList<>(project.getAssignedUsers());
        model.addAttribute("task", new Task());
        model.addAttribute("project", project);
        model.addAttribute("members", projectMembers);
        return "task-create";
    }

    @PostMapping("/create")
    public String createTask(@ModelAttribute Task task,
                             @RequestParam Long projectId,
                             @RequestParam List<Long> assignedUserIds) {

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        Set<User> users = assignedUserIds.stream()
                .map(id -> userService.getUserById(id)
                        .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje: " + id)))
                .collect(Collectors.toSet());

        task.setProject(project);
        task.setAssignedUsers(users);
        task.setCreatedAt(LocalDateTime.now());

        taskService.saveTask(task);

        return "redirect:/tasks/project/" + projectId;
    }

    @GetMapping("/project/{projectId}/filter")
    public String filterTasksByStatus(@PathVariable Long projectId,
                                      @RequestParam(required = false) String status,
                                      Model model) {
        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));
        List<Task> tasks = taskService.getTasksByProject(project);
        if (status != null && !status.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> status.equals(task.getStatus()))
                    .toList();
        }
        model.addAttribute("tasks", tasks);
        model.addAttribute("project", project);
        return "tasks";
    }

    // src/main/java/com/example/demo/controller/TaskController.java - FRAGMENT DO ZMIANY
    @PostMapping("/update-status/{taskId}")
    public String updateStatus(@PathVariable Long taskId,
                               @RequestParam String status,
                               @RequestParam(required = false) String returnTo, // DODAJ TEN PARAMETR
                               @AuthenticationPrincipal UserDetails userDetails) {

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        if (!task.getAssignedUsers().contains(currentUser)) {
            throw new RuntimeException("Nie masz uprawnień do zmiany statusu tego zadania");
        }

        task.setStatus(status);
        taskService.saveTask(task);

        // DODAJ LOGIKĘ PRZEKIEROWANIA
        if ("task-view".equals(returnTo)) {
            return "redirect:/tasks/view/" + taskId;
        } else {
            return "redirect:/tasks/project/" + task.getProject().getId();
        }
    }

    @GetMapping("/project/{projectId}")
    public String getTasksByProject(@PathVariable Long projectId,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    Model model) {
        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));
        List<Task> tasks = taskService.getTasksByProject(project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("project", project);
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "tasks";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        List<Task> allTasks = taskService.getAllTasks();
        List<Task> assignedTasks = allTasks.stream()
                .filter(task -> task.getAssignedUsers().contains(user))
                .collect(Collectors.toList());

        model.addAttribute("username", user.getUsername());
        model.addAttribute("tasks", assignedTasks);

        return "dashboard";
    }

    @GetMapping("/{id}")
    public String getTaskDetails(@PathVariable Long id, Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        Task task = taskService.getTaskById(id)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        model.addAttribute("task", task);
        model.addAttribute("username", userDetails.getUsername());

        return "task-details";
    }
}