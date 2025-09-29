// src/main/java/com/example/demo/controller/TaskViewController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    @Autowired private TaskService taskService;
    @Autowired private CommentService commentService;
    @Autowired private FileService fileService;
    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private ProjectMemberService memberService;

    @GetMapping("/view/{id}")
    public String viewTask(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.findById(id);
        Project project = task.getProject();

        // Sprawdź dostęp do projektu
        Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, currentUser);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Brak dostępu do projektu");
        }

        ProjectRole userRole = memberOpt.get().getRole();
        List<Task> projectTasks = taskService.findAllByProject(project);

        // POPRAWIONE: używamy getCommentsByTask zamiast getCommentsForTask
        List<Comment> comments = commentService.getCommentsByTask(task);
        List<UploadedFile> files = fileService.getFilesByTask(task);

        // DODAJ DOSTĘPNYCH UŻYTKOWNIKÓW DO PRZYPISANIA (tylko dla adminów)
        List<User> availableUsersToAssign = null;
        Set<Long> assignedUserIds = null;

        if (userRole == ProjectRole.ADMIN) {
            // Pobierz wszystkich członków projektu (Admin i Member, nie Viewer)
            List<ProjectMember> projectMembers = memberService.getProjectMembers(project);
            List<User> allProjectUsers = projectMembers.stream()
                    .filter(member -> member.getRole() != ProjectRole.VIEWER)
                    .map(ProjectMember::getUser)
                    .collect(Collectors.toList());

            // Pobierz ID już przypisanych użytkowników
            assignedUserIds = task.getAssignedUsers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            // Filtruj użytkowników, którzy jeszcze nie są przypisani
            availableUsersToAssign = allProjectUsers.stream()
                    .filter(user -> !task.getAssignedUsers().contains(user))
                    .collect(Collectors.toList());

            System.out.println("Dostępnych użytkowników do przypisania: " + availableUsersToAssign.size());
            System.out.println("Przypisanych użytkowników: " + task.getAssignedUsers().size());
        }

        model.addAttribute("task", task);
        model.addAttribute("project", project);
        model.addAttribute("projectTasks", projectTasks);
        model.addAttribute("comments", comments);
        model.addAttribute("files", files);
        model.addAttribute("currentUsername", userDetails.getUsername());
        model.addAttribute("userRole", userRole);
        model.addAttribute("isAdmin", userRole == ProjectRole.ADMIN);

        // NOWE ATRYBUTY dla zarządzania użytkownikami
        model.addAttribute("availableUsersToAssign", availableUsersToAssign);
        model.addAttribute("assignedUserIds", assignedUserIds);

        return "task-view";
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @RequestParam String commentText,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.findById(id);

        // Sprawdź uprawnienia
        ProjectRole userRole = memberService.getProjectMember(task.getProject(), currentUser)
                .map(ProjectMember::getRole).orElse(null);

        if (userRole == null || userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Brak uprawnień do dodawania komentarzy");
        }

        // POPRAWIONE: używamy addCommentToTask z Task, String, User
        commentService.addCommentToTask(task, commentText, currentUser);

        return "redirect:/tasks/view/" + id;
    }

    @PostMapping("/{id}/upload")
    public String uploadFile(@PathVariable Long id,
                             @RequestParam("file") MultipartFile file,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.findById(id);

        // Sprawdź uprawnienia
        ProjectRole userRole = memberService.getProjectMember(task.getProject(), currentUser)
                .map(ProjectMember::getRole).orElse(null);

        if (userRole == null || userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Brak uprawnień do dodawania plików");
        }

        System.out.println("Próba zapisu pliku: " + file.getOriginalFilename());
        fileService.storeFile(task, file, currentUser);

        return "redirect:/tasks/view/" + id;
    }

    // METODY USUWANIA
    @PostMapping("/comments/delete/{commentId}")
    public String deleteComment(@PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        // POPRAWIONE: getCommentById zwraca Optional
        Comment comment = commentService.getCommentById(commentId)
                .orElseThrow(() -> new RuntimeException("Komentarz nie istnieje"));

        Task task = comment.getTask();
        Project project = task.getProject();

        boolean isAdmin = memberService.isProjectAdmin(project, currentUser);
        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().equals(currentUser);

        if (!isAdmin && !isAuthor) {
            throw new RuntimeException("Brak uprawnień do usuwania tego komentarza");
        }

        commentService.deleteComment(commentId);
        System.out.println("Użytkownik " + currentUser.getUsername() + " usunął komentarz ID: " + commentId);

        return "redirect:/tasks/view/" + task.getId();
    }

    @PostMapping("/files/delete/{fileId}")
    public String deleteFile(@PathVariable Long fileId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        // POPRAWIONE: getFileById zwraca Optional
        UploadedFile file = fileService.getFileById(fileId)
                .orElseThrow(() -> new RuntimeException("Plik nie istnieje"));

        Task task = file.getTask();
        Project project = task.getProject();

        boolean isAdmin = memberService.isProjectAdmin(project, currentUser);
        boolean isAuthor = file.getUploadedBy() != null && file.getUploadedBy().equals(currentUser);

        if (!isAdmin && !isAuthor) {
            throw new RuntimeException("Brak uprawnień do usuwania tego pliku");
        }

        fileService.deleteFile(fileId);
        System.out.println("Użytkownik " + currentUser.getUsername() + " usunął plik: " + file.getOriginalName());

        return "redirect:/tasks/view/" + task.getId();
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) {
        // POPRAWIONE: getFileById zwraca Optional
        UploadedFile file = fileService.getFileById(id)
                .orElse(null);

        if (file == null || file.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(file.getData());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getData().length)
                .body(resource);
    }

    // Dodatkowe metody dla zarządzania użytkownikami w zadaniu
    @PostMapping("/{taskId}/assign-user")
    public String assignUserToTask(@PathVariable Long taskId,
                                   @RequestParam Long userId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.findById(taskId);

        // Sprawdź czy użytkownik jest adminem projektu
        if (!memberService.isProjectAdmin(task.getProject(), currentUser)) {
            throw new RuntimeException("Tylko administrator projektu może przypisywać użytkowników");
        }

        User userToAssign = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Przypisz użytkownika
        task.getAssignedUsers().add(userToAssign);
        taskService.saveTask(task);

        System.out.println("Przypisano użytkownika " + userToAssign.getUsername() + " do zadania " + task.getTitle());

        return "redirect:/tasks/view/" + taskId;
    }

    @PostMapping("/{taskId}/unassign-user/{userId}")
    public String unassignUserFromTask(@PathVariable Long taskId,
                                       @PathVariable Long userId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Task task = taskService.findById(taskId);

        // Sprawdź czy użytkownik jest adminem projektu
        if (!memberService.isProjectAdmin(task.getProject(), currentUser)) {
            throw new RuntimeException("Tylko administrator projektu może odpisywać użytkowników");
        }

        User userToUnassign = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Odpisz użytkownika
        task.getAssignedUsers().remove(userToUnassign);
        taskService.saveTask(task);

        System.out.println("Odpisano użytkownika " + userToUnassign.getUsername() + " z zadania " + task.getTitle());

        return "redirect:/tasks/view/" + taskId;
    }
}