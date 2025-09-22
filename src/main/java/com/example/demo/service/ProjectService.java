// src/main/java/com/example/demo/service/ProjectService.java - DODANIE METODY DLA ADMINA
package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.User;
import com.example.demo.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired(required = false)
    private ProjectMemberService memberService;

    @Autowired(required = false)
    private TaskService taskService;

    @Autowired(required = false)
    private MessageService messageService;

    // Tworzenie projektu
    public Project createProject(String name, String description, User creator) {
        Project project = new Project(name, description, creator);
        return projectRepository.save(project);
    }

    public Project getProjectByName(String name) {
        return projectRepository.findByName(name);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }

    // NOWA METODA - Usuwanie projektu przez super admina
    @Transactional
    public void deleteProjectByAdmin(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projekt nie istnieje"));

        String projectName = project.getName();

        try {
            System.out.println("🔴 SUPER ADMIN usuwa projekt: " + projectName + " (ID: " + projectId + ")");

            // 1. Usuń wszystkie zadania projektu (wraz z komentarzami i plikami)
            if (taskService != null) {
                // Pobierz wszystkie zadania i usuń je kaskadowo
                taskService.findAllByProject(project).forEach(task -> {
                    try {
                        taskService.deleteTask(task.getId());
                    } catch (Exception e) {
                        System.err.println("Błąd usuwania zadania ID " + task.getId() + ": " + e.getMessage());
                    }
                });
            }

            // 2. Usuń wszystkie wiadomości w czacie projektu
            if (messageService != null) {
                try {
                    messageService.deleteAllProjectMessages(project);
                } catch (Exception e) {
                    System.err.println("Błąd usuwania wiadomości projektu: " + e.getMessage());
                }
            }

            // 3. Usuń wszystkich członków projektu
            if (memberService != null) {
                try {
                    memberService.getProjectMembers(project).forEach(member -> {
                        try {
                            memberService.removeMemberFromProject(project, member.getUser());
                        } catch (Exception e) {
                            System.err.println("Błąd usuwania członka " + member.getUser().getUsername() + ": " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Błąd usuwania członków projektu: " + e.getMessage());
                }
            }

            // 4. Usuń sam projekt
            projectRepository.deleteById(projectId);

            System.out.println("✅ Pomyślnie usunięto projekt: " + projectName + " wraz z wszystkimi powiązanymi danymi");

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania projektu: " + projectName);
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania projektu: " + e.getMessage(), e);
        }
    }
}