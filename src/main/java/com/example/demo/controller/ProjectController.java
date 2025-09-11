// src/main/java/com/example/demo/controller/ProjectController.java - USUŃ TE METODY
package com.example.demo.controller;

import com.example.demo.model.Project;
import com.example.demo.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    // Tworzenie nowego projektu (POST)
    @PostMapping("/create")
    public Project createProject(@RequestBody Project project) {
        // ZMIEŃ NA NOWĄ SYGNATURĘ (ale potrzebujesz User creator)
        // return projectService.createProject(project.getName(), project.getDescription(), creator);
        throw new RuntimeException("Użyj /projects/create zamiast API");
    }

    // USUŃ TE METODY - używamy teraz ProjectMemberService
    /*
    @PostMapping("/{projectId}/addUser/{userId}")
    public Project addUserToProject(@PathVariable Long projectId, @PathVariable Long userId) {
        return projectService.addUserToProject(projectId, userId);
    }

    @DeleteMapping("/{projectId}/removeUser/{userId}")
    public Project removeUserFromProject(@PathVariable Long projectId, @PathVariable Long userId) {
        return projectService.removeUserFromProject(projectId, userId);
    }
    */

    // Pobranie projektu po ID (GET)
    @GetMapping("/{id}")
    public Project getProject(@PathVariable Long id) {
        return projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Projekt o ID " + id + " nie istnieje."));
    }

    // Pobranie wszystkich projektów (GET)
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }
}