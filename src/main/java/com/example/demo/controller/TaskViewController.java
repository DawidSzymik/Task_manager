// src/main/java/com/example/demo/controller/TaskViewController.java - POPRAWIONY
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
        List<Comment> comments = commentService.getCommentsForTask(id);
        List<UploadedFile> files = fileService.getFilesForTask(id);

        model.addAttribute("task", task);
        model.addAttribute("project", project);
        model.addAttribute("projectTasks", projectTasks);
        model.addAttribute("comments", comments);
        model.addAttribute("files", files);
        model.addAttribute("currentUsername", userDetails.getUsername());
        model.addAttribute("userRole", userRole);
        model.addAttribute("isAdmin", userRole == ProjectRole.ADMIN);

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

        commentService.addCommentToTask(id, commentText, userDetails.getUsername());
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
        fileService.storeFileForTask(id, file, userDetails.getUsername());
        return "redirect:/tasks/view/" + id;
    }

    // METODY USUWANIA - BEZ ZMIAN
    @PostMapping("/comments/delete/{commentId}")
    public String deleteComment(@PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        Comment comment = commentService.getCommentById(commentId);

        if (comment == null) {
            throw new RuntimeException("Komentarz nie istnieje");
        }

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
        UploadedFile file = fileService.getFileById(fileId);

        if (file == null) {
            throw new RuntimeException("Plik nie istnieje");
        }

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
        UploadedFile file = fileService.getFileById(id);

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
}