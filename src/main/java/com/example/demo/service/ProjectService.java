// src/main/java/com/example/demo/service/ProjectService.java
package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // Existing methods
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }

    public List<Project> getProjectsByCreator(User creator) {
        return projectRepository.findByCreatedBy(creator);
    }

    public Optional<Project> findByName(String name) {
        return projectRepository.findByName(name);
    }

    // DODANA BRAKUJĄCA METODA
    public Project createProject(String name, String description, User createdBy) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setCreatedBy(createdBy);
        project.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    // NOWA METODA - Usuwanie projektu przez administratora
    @Transactional
    public void deleteProjectByAdmin(Long projectId) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);

            if (projectOpt.isEmpty()) {
                throw new RuntimeException("Projekt o ID " + projectId + " nie istnieje");
            }

            Project project = projectOpt.get();

            System.out.println("🗑️ Admin usuwa projekt: " + project.getName() + " (ID: " + projectId + ")");

            // 1. Usuń wszystkie zadania związane z projektem i ich dane
            List<Task> projectTasks = taskRepository.findByProject(project);
            for (Task task : projectTasks) {
                // Usuń komentarze do zadań
                commentRepository.deleteByTask(task);

                // Usuń pliki związane z zadaniami
                uploadedFileRepository.deleteByTask(task);

                // Wyczyść relacje many-to-many z użytkownikami
                task.getAssignedUsers().clear();
                taskRepository.save(task);

                // Usuń samo zadanie
                taskRepository.delete(task);

                System.out.println("  🗑️ Usunięto zadanie: " + task.getTitle());
            }

            // 2. Usuń wszystkie wiadomości w czacie projektu
            messageRepository.deleteByProject(project);
            System.out.println("  🗑️ Usunięto wiadomości czatu");

            // 3. Usuń wszystkich członków projektu
            projectMemberRepository.deleteByProject(project);
            System.out.println("  🗑️ Usunięto członków projektu");

            // 4. Usuń powiadomienia związane z projektem (jeśli istnieją)
            try {
                notificationRepository.deleteByRelatedEntityId(projectId);
                System.out.println("  🗑️ Usunięto powiadomienia");
            } catch (Exception e) {
                System.out.println("  ⚠️ Błąd usuwania powiadomień (może nie istnieć metoda): " + e.getMessage());
            }

            // 5. Na końcu usuń sam projekt
            projectRepository.delete(project);

            System.out.println("✅ Pomyślnie usunięto projekt " + project.getName() + " wraz z wszystkimi danymi");

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania projektu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się usunąć projektu: " + e.getMessage());
        }
    }
}