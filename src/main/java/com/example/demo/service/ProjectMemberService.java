// src/main/java/com/example/demo/service/ProjectMemberService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectMemberRepository memberRepository;

    // Dodaj członka do projektu
    public ProjectMember addMemberToProject(Project project, User user, ProjectRole role) {
        // Sprawdź czy już jest członkiem
        if (memberRepository.existsByProjectAndUser(project, user)) {
            System.out.println("Użytkownik już jest członkiem projektu");
            return memberRepository.findByProjectAndUser(project, user).orElse(null);
        }

        ProjectMember member = new ProjectMember(project, user, role);
        ProjectMember saved = memberRepository.save(member);
        System.out.println("Zapisano członkostwo: " + saved.getUser().getUsername() + " w projekcie " + saved.getProject().getName());
        return saved;
    }

    // Usuń członka z projektu
    public void removeMemberFromProject(Project project, User user) {
        Optional<ProjectMember> member = memberRepository.findByProjectAndUser(project, user);
        if (member.isPresent()) {
            memberRepository.delete(member.get());
        }
    }

    // Zmień rolę użytkownika
    public void changeUserRole(Project project, User user, ProjectRole newRole, User changedBy) {
        Optional<ProjectMember> memberOpt = memberRepository.findByProjectAndUser(project, user);

        if (memberOpt.isPresent()) {
            ProjectMember member = memberOpt.get();

            // Nie można zdegradować twórcy projektu
            if (project.getCreatedBy().equals(user) && newRole != ProjectRole.ADMIN) {
                throw new RuntimeException("Nie można zmienić roli twórcy projektu");
            }

            member.setRole(newRole);
            memberRepository.save(member);
        }
    }

    // Pobierz członków projektu
    public List<ProjectMember> getProjectMembers(Project project) {
        return memberRepository.findByProject(project);
    }

    // Pobierz projekty użytkownika
    public List<ProjectMember> getUserProjects(User user) {
        return memberRepository.findByUser(user);
    }

    // Pobierz członkostwo w projekcie
    public Optional<ProjectMember> getProjectMember(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user);
    }

    // Sprawdź czy jest adminem
    public boolean isProjectAdmin(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user)
                .map(member -> member.getRole() == ProjectRole.ADMIN)
                .orElse(false);
    }

    // Sprawdź czy jest członkiem
    public boolean isProjectMember(Project project, User user) {
        return memberRepository.existsByProjectAndUser(project, user);
    }
}