package com.example.demo.controller;
import java.util.Set;
import java.util.HashSet;

import com.example.demo.model.Task;
import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.service.TaskService;
import com.example.demo.service.TeamService;
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
    private final TeamService teamService;
    private final UserService userService;

    public TaskController(TaskService taskService, TeamService teamService, UserService userService) {
        this.taskService = taskService;
        this.teamService = teamService;
        this.userService = userService;
    }



    @GetMapping("/create/{teamId}")
    public String createTaskForm(@PathVariable Long teamId, Model model) {
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new RuntimeException("Zespół nie istnieje"));
        List<User> teamMembers = new ArrayList<>(team.getUsers());
        model.addAttribute("task", new Task());
        model.addAttribute("team", team);
        model.addAttribute("members", teamMembers);
        return "task-create";
    }

    @PostMapping("/create")
    public String createTask(@ModelAttribute Task task,
                             @RequestParam Long teamId,
                             @RequestParam List<Long> assignedUserIds) {

        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new RuntimeException("Zespół nie istnieje"));

        Set<User> users = assignedUserIds.stream()
                .map(id -> userService.getUserById(id)
                        .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje: " + id)))
                .collect(Collectors.toSet());

        task.setTeam(team);
        task.setAssignedUsers(users);
        task.setCreatedAt(LocalDateTime.now());

        taskService.saveTask(task);

        return "redirect:/tasks/team/" + teamId;
    }
    @GetMapping("/team/{teamId}/filter")
    public String filterTasksByStatus(@PathVariable Long teamId,
                                      @RequestParam(required = false) String status,
                                      Model model) {
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new RuntimeException("Zespół nie istnieje"));
        List<Task> tasks = taskService.getTasksByTeam(team);
        if (status != null && !status.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> status.equals(task.getStatus()))
                    .toList();
        }
        model.addAttribute("tasks", tasks);
        model.addAttribute("team", team);
        return "tasks";
    }
    @PostMapping("/update-status/{taskId}")
    public String updateStatus(@PathVariable Long taskId,
                               @RequestParam String status,
                               @AuthenticationPrincipal UserDetails userDetails) {

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        // sprawdzamy, czy użytkownik jest przypisany do zadania
        if (!task.getAssignedUsers().contains(currentUser)) {
            throw new RuntimeException("Nie masz uprawnień do zmiany statusu tego zadania");
        }

        task.setStatus(status);
        taskService.saveTask(task);

        return "redirect:/tasks/team/" + task.getTeam().getId();
    }
    @GetMapping("/team/{teamId}")
    public String getTasksByTeam(@PathVariable Long teamId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new RuntimeException("Zespół nie istnieje"));
        List<Task> tasks = taskService.getTasksByTeam(team);
        model.addAttribute("tasks", tasks);
        model.addAttribute("team", team);
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "tasks";
    }
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        List<Task> allTasks = taskService.getAllTasks(); // potrzebujemy tej metody w service
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

        return "task-details"; // stworzysz ten plik HTML
    }



}
