// src/main/java/com/example/demo/service/ProjectService.java
package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // Tworzenie nowego projektu
    public Project createProject(String name, String description) {
        Project project = new Project(name, description);
        return projectRepository.save(project);
    }

    // Pobranie wszystkich projektów
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Pobranie projektu po ID
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    // Dodanie użytkownika do projektu
    public Project addUserToProject(Long projectId, Long userId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (projectOpt.isPresent() && userOpt.isPresent()) {
            Project project = projectOpt.get();
            User user = userOpt.get();
            project.addUser(user);
            return projectRepository.save(project);
        }
        return null;
    }

    // Usunięcie użytkownika z projektu
    public Project removeUserFromProject(Long projectId, Long userId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (projectOpt.isPresent() && userOpt.isPresent()) {
            Project project = projectOpt.get();
            User user = userOpt.get();
            project.removeUser(user);
            return projectRepository.save(project);
        }
        return null;
    }

    // Projekty użytkownika
    public List<Project> getProjectsByUser(User user) {
        return projectRepository.findByAssignedUser(user);
    }

    // Aktualizacja projektu
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    // Usunięcie projektu
    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }
}
