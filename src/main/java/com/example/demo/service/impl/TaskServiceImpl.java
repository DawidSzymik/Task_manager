// src/main/java/com/example/demo/service/impl/TaskServiceImpl.java
package com.example.demo.service.impl;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.TaskService;
import com.example.demo.service.CommentService;
import com.example.demo.service.FileService;
import com.example.demo.service.StatusChangeRequestService;
import com.example.demo.service.TaskProposalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private FileService fileService;

    // Te zależności pozostają (required = false, żeby uniknąć błędów)
    @Autowired(required = false)
    private StatusChangeRequestService statusChangeRequestService;

    @Autowired(required = false)
    private TaskProposalService taskProposalService;

    @Override
    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public List<Task> getTasksByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));

        String taskTitle = task.getTitle();

        try {
            System.out.println("Rozpoczynam usuwanie zadania: " + taskTitle + " (ID: " + id + ")");

            // 1. Usuń wszystkie status change requests (NAJWAŻNIEJSZE!)
            if (statusChangeRequestService != null) {
                statusChangeRequestService.deleteRequestsForTask(id);
            } else {
                System.out.println("StatusChangeRequestService nie jest dostępny");
            }

            // 2. Usuń wszystkie task proposals związane z tym zadaniem
            if (taskProposalService != null) {
                taskProposalService.deleteProposalsForTask(id);
            } else {
                System.out.println("TaskProposalService nie jest dostępny");
            }

            // 3. Usuń wszystkie komentarze
            commentService.deleteCommentsForTask(id);

            // 4. Usuń wszystkie pliki
            fileService.deleteFilesForTask(id);

            // 5. Usuń zadanie (relacje Many-to-Many z użytkownikami zostaną automatycznie usunięte)
            taskRepository.deleteById(id);

            System.out.println("Pomyślnie usunięto zadanie: " + taskTitle + " wraz z wszystkimi powiązanymi danymi");

        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania zadania: " + taskTitle);
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania zadania: " + e.getMessage(), e);
        }
    }

    @Override
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zadanie nie istnieje"));
    }

    @Override
    public List<Task> findAllByProject(Project project) {
        return taskRepository.findByProject(project);
    }

    // Usuwanie wszystkich zadań projektu (wywoływane przy usuwaniu projektu)
    @Transactional
    public void deleteAllTasksForProject(Long projectId) {
        Project project = new Project();
        project.setId(projectId);

        List<Task> tasks = taskRepository.findByProject(project);

        System.out.println("Usuwam " + tasks.size() + " zadań dla projektu ID: " + projectId);

        for (Task task : tasks) {
            deleteTask(task.getId());
        }

        System.out.println("Usunięto wszystkie zadania dla projektu ID: " + projectId);
    }
}