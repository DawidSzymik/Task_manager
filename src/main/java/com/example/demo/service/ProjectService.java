// src/main/java/com/example/demo/service/ProjectService.java - POPRAWIONY
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

    // USUŃ TO - powoduje cykliczną zależność
    // @Autowired
    // private ProjectMemberService memberService;

    // ZMIEŃ metodę createProject - nie dodawaj członka tutaj
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
}