package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberService memberService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @GetMapping("/create/{projectId}")
    public String createTaskForm(@PathVariable Long projectId, Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        // Sprawdź czy użytkownik jest adminem projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Tylko admini mogą tworzyć zadania");
        }

        // Pobierz członków projektu (nie viewerów)
        List<ProjectMember> projectMembers = memberService.getProjectMembers(project);
        List<User> availableUsers = projectMembers.stream()
                .filter(member -> member.getRole() != ProjectRole.VIEWER)
                .map(ProjectMember::getUser)
                .collect(Collectors.toList());

        model.addAttribute("task", new Task());
        model.addAttribute("project", project);
        model.addAttribute("members", availableUsers);
        return "task-create";
    }

    @PostMapping("/create")
    public String createTask(@RequestParam String title,
                             @RequestParam String description,
                             @RequestParam(required = false) String deadline,
                             @RequestParam Long projectId,
                             @RequestParam(required = false) List<Long> assignedUserIds,
                             @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        // Sprawdź uprawnienia
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Tylko admini mogą tworzyć zadania");
        }

        // Stwórz zadanie
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setProject(project);
        task.setCreatedAt(LocalDateTime.now());
        task.setStatus("TODO");

        // Ustaw deadline jeśli podano
        if (deadline != null && !deadline.isEmpty()) {
            task.setDeadline(LocalDateTime.parse(deadline));
        }

        // Przypisz użytkowników jeśli zostali wybrani
        Set<User> assignedUsers = new HashSet<>();
        if (assignedUserIds != null && !assignedUserIds.isEmpty()) {
            assignedUsers = assignedUserIds.stream()
                    .map(id -> userService.getUserById(id)
                            .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje: " + id)))
                    .collect(Collectors.toSet());
        }
        task.setAssignedUsers(assignedUsers);

        taskService.saveTask(task);
        return "redirect:/tasks/project/" + projectId;
    }

    // NOWA METODA - Dodawanie użytkownika do zadania
    @PostMapping("/assign/{taskId}")
    public String assignUserToTask(@PathVariable Long taskId,
                                   @RequestParam Long userId,
                                   @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        Project project = task.getProject();

        // Sprawdź czy użytkownik jest adminem projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Tylko admini mogą przypisywać użytkowników do zadań");
        }

        User userToAssign = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Sprawdź czy użytkownik do przypisania jest członkiem projektu i nie jest viewerem
        Optional<ProjectMember> targetMemberOpt = memberService.getProjectMember(project, userToAssign);
        if (targetMemberOpt.isEmpty() || targetMemberOpt.get().getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Można przypisać tylko członków projektu (nie viewerów)");
        }

        // Dodaj użytkownika do zadania jeśli jeszcze nie jest przypisany
        Set<User> assignedUsers = task.getAssignedUsers();
        if (!assignedUsers.contains(userToAssign)) {
            assignedUsers.add(userToAssign);
            task.setAssignedUsers(assignedUsers);
            taskService.saveTask(task);

            System.out.println("Admin " + currentUser.getUsername() + " przypisał użytkownika "
                    + userToAssign.getUsername() + " do zadania: " + task.getTitle());

            // NOWE: Wyślij powiadomienie do przypisanego użytkownika
            try {
                eventPublisher.publishEvent(new NotificationEvent(
                        userToAssign,
                        "📋 Przypisano Cię do zadania",
                        "Zostałeś przypisany do zadania \"" + task.getTitle() + "\" w projekcie \"" + project.getName() + "\"",
                        NotificationType.TASK_ASSIGNED,
                        task.getId(),
                        "/tasks/view/" + task.getId()
                ));
            } catch (Exception e) {
                System.err.println("Błąd wysyłania powiadomienia o przypisaniu: " + e.getMessage());
            }
        }

        return "redirect:/tasks/view/" + taskId;
    }

    // NOWA METODA - Usuwanie użytkownika z zadania
    @PostMapping("/unassign/{taskId}")
    public String unassignUserFromTask(@PathVariable Long taskId,
                                       @RequestParam Long userId,
                                       @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        Project project = task.getProject();

        // Sprawdź czy użytkownik jest adminem projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Tylko admini mogą odpisywać użytkowników z zadań");
        }

        User userToUnassign = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Usuń użytkownika z zadania
        Set<User> assignedUsers = task.getAssignedUsers();
        if (assignedUsers.remove(userToUnassign)) {
            task.setAssignedUsers(assignedUsers);
            taskService.saveTask(task);

            System.out.println("Admin " + currentUser.getUsername() + " odpisał użytkownika "
                    + userToUnassign.getUsername() + " z zadania: " + task.getTitle());
        }

        return "redirect:/tasks/view/" + taskId;
    }

    // NOWA METODA - USUWANIE ZADANIA
    @PostMapping("/delete/{taskId}")
    public String deleteTask(@PathVariable Long taskId,
                             @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        Project project = task.getProject();

        // Sprawdź czy użytkownik jest adminem projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Tylko admini mogą usuwać zadania");
        }

        // Usuń zadanie (kaskadowe usuwanie komentarzy i plików)
        taskService.deleteTask(taskId);

        System.out.println("Admin " + currentUser.getUsername() + " usunął zadanie: " + task.getTitle());

        return "redirect:/tasks/project/" + project.getId();
    }

    @GetMapping("/project/{projectId}/filter")
    public String filterTasksByStatus(@PathVariable Long projectId,
                                      @RequestParam(required = false) String status,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      Model model) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        // Sprawdź czy użytkownik ma dostęp do projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        List<Task> tasks = taskService.getTasksByProject(project);
        if (status != null && !status.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> status.equals(task.getStatus()))
                    .collect(Collectors.toList());
        }

        ProjectRole userRole = memberOpt.get().getRole();

        model.addAttribute("tasks", tasks);
        model.addAttribute("project", project);
        model.addAttribute("currentUsername", userDetails.getUsername());
        model.addAttribute("userRole", userRole);
        model.addAttribute("isAdmin", userRole == ProjectRole.ADMIN);

        return "tasks";
    }

    @PostMapping("/update-status/{taskId}")
    public String updateStatus(@PathVariable Long taskId,
                               @RequestParam String status,
                               @RequestParam(required = false) String returnTo,
                               @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        // Sprawdź rolę w projekcie
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(task.getProject(), currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        ProjectRole userRole = memberOpt.get().getRole();

        if (userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Brak uprawnień do zmiany statusu");
        }

        // Admin może zmieniać bezpośrednio
        if (userRole == ProjectRole.ADMIN) {
            task.setStatus(status);
            taskService.saveTask(task);
        }
        // Member składa prośbę o zmianę (jeśli masz StatusChangeRequestService)
        // else {
        //     statusChangeRequestService.requestStatusChange(task, status, currentUser);
        // }

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
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        // Sprawdź czy użytkownik ma dostęp do projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        List<Task> tasks = taskService.getTasksByProject(project);
        ProjectRole userRole = memberOpt.get().getRole();

        model.addAttribute("tasks", tasks);
        model.addAttribute("project", project);
        model.addAttribute("currentUsername", userDetails.getUsername());
        model.addAttribute("userRole", userRole);
        model.addAttribute("isAdmin", userRole == ProjectRole.ADMIN);

        return "tasks";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        // Pobierz wszystkie projekty użytkownika
        List<ProjectMember> userMemberships = memberService.getUserProjects(user);
        List<Project> userProjects = userMemberships.stream()
                .map(ProjectMember::getProject)
                .collect(Collectors.toList());

        // Pobierz zadania z projektów użytkownika
        List<Task> allTasks = userProjects.stream()
                .flatMap(project -> taskService.getTasksByProject(project).stream())
                .collect(Collectors.toList());

        // Filtruj zadania przypisane do użytkownika
        List<Task> assignedTasks = allTasks.stream()
                .filter(task -> task.getAssignedUsers().contains(user))
                .collect(Collectors.toList());

        model.addAttribute("username", user.getUsername());
        model.addAttribute("tasks", assignedTasks);
        model.addAttribute("userProjects", userProjects);

        return "dashboard";
    }

    // Event class dla powiadomień
    public static class NotificationEvent {
        private final User user;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final Long relatedId;
        private final String actionUrl;

        public NotificationEvent(User user, String title, String message, NotificationType type, Long relatedId, String actionUrl) {
            this.user = user;
            this.title = title;
            this.message = message;
            this.type = type;
            this.relatedId = relatedId;
            this.actionUrl = actionUrl;
        }

        public User getUser() { return user; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public Long getRelatedId() { return relatedId; }
        public String getActionUrl() { return actionUrl; }
    }
}