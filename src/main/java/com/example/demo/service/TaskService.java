// src/main/java/com/example/demo/service/TaskService.java
package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.NotificationType;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    @Lazy
    private UserService userService;

    public Task findById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));
    }

    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    public List<Task> getTasksByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    public List<Task> getTasksByAssignedUser(User user) {
        return taskRepository.findByAssignedUsersContaining(user);
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = findById(taskId);

        // Usuń komentarze
        commentRepository.deleteByTask(task);

        // Usuń pliki
        uploadedFileRepository.deleteByTask(task);

        // Usuń zadanie
        taskRepository.delete(task);
    }

    @Transactional
    public void deleteCommentsForTask(Task task) {
        commentRepository.deleteByTask(task);
    }

    public List<Task> findAllByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
}