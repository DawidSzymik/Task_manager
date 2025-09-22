// src/main/java/com/example/demo/service/impl/TaskServiceImpl.java
package com.example.demo.service.impl;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.TaskService;
import com.example.demo.service.CommentService;
import com.example.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired(required = false)
    private CommentService commentService;

    @Autowired(required = false)
    private FileService fileService;

    @Override
    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<Task> getTasksByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    @Override
    public List<Task> findAllByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    @Override
    public List<Task> findByAssignedTo(User user) {
        return taskRepository.findByAssignedTo(user);
    }

    @Override
    @Transactional
    public void unassignUserFromAllTasks(User user) {
        try {
            List<Task> userTasks = taskRepository.findByAssignedTo(user);

            System.out.println("Odpisywanie użytkownika " + user.getUsername() + " od " + userTasks.size() + " zadań");

            for (Task task : userTasks) {
                task.setAssignedTo(null);
                taskRepository.save(task);
            }

            System.out.println("✅ Pomyślnie odpisano użytkownika od wszystkich zadań");

        } catch (Exception e) {
            System.err.println("❌ Błąd odpisywania użytkownika od zadań: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        try {
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

            String taskTitle = task.getTitle();
            System.out.println("Rozpoczynam usuwanie zadania: " + taskTitle + " (ID: " + id + ")");

            // 1. Usuń komentarze (jeśli serwis istnieje)
            if (commentService != null) {
                commentService.deleteCommentsForTask(id);
            }

            // 2. Usuń pliki (jeśli serwis istnieje)
            if (fileService != null) {
                fileService.deleteFilesForTask(id);
            }

            // 3. Usuń zadanie
            taskRepository.deleteById(id);

            System.out.println("✅ Pomyślnie usunięto zadanie: " + taskTitle);

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania zadania " + id + ": " + e.getMessage());
            throw new RuntimeException("Błąd podczas usuwania zadania: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAllTasksForProject(Long projectId) {
        try {
            Project project = new Project();
            project.setId(projectId);

            List<Task> tasks = taskRepository.findByProject(project);
            System.out.println("Usuwam " + tasks.size() + " zadań dla projektu ID: " + projectId);

            for (Task task : tasks) {
                deleteTask(task.getId());
            }

            System.out.println("✅ Usunięto wszystkie zadania dla projektu ID: " + projectId);

        } catch (Exception e) {
            System.err.println("❌ Błąd usuwania zadań projektu " + projectId + ": " + e.getMessage());
        }
    }
}