package com.example.demo.controller;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.Team;
import com.example.demo.model.UploadedFile;
import com.example.demo.service.CommentService;
import com.example.demo.service.FileService;
import com.example.demo.service.TaskService;
import com.example.demo.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    @Autowired private TaskService taskService;
    @Autowired private CommentService commentService;
    @Autowired private FileService fileService;
    @Autowired private TeamService teamService;

    @GetMapping("/view/{id}")
    public String viewTask(@PathVariable Long id, Model model) {
        Task task = taskService.findById(id);
        Team team = task.getTeam();

        List<Task> teamTasks = taskService.findAllByTeam(team);
        List<Comment> comments = commentService.getCommentsForTask(id);
        List<UploadedFile> files = fileService.getFilesForTask(id);

        model.addAttribute("task", task);
        model.addAttribute("team", team);
        model.addAttribute("teamTasks", teamTasks);
        model.addAttribute("comments", comments);
        model.addAttribute("files", files);

        return "task-view";
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id, @RequestParam String commentText) {
        commentService.addCommentToTask(id, commentText);
        return "redirect:/tasks/view/" + id;
    }

    @PostMapping("/{id}/upload")
    public String uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        System.out.println("Próba zapisu pliku: " + file.getOriginalFilename());
        fileService.storeFileForTask(id, file);
        return "redirect:/tasks/view/" + id;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) {
        UploadedFile file = fileService.getFileById(id);
        ByteArrayResource resource = new ByteArrayResource(file.getData());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getData().length)
                .body(resource);
    }
}
