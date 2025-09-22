// src/main/java/com/example/demo/service/ProjectMemberService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MessageService messageService;

    // Dodaj użytkownika do projektu
    @Transactional
    public ProjectMember addMemberToProject(Project project, User user, ProjectRole role) {
        // Sprawdź czy już nie jest członkiem
        Optional<ProjectMember> existingMember = projectMemberRepository.findByProjectAndUser(project, user);
        if (existingMember.isPresent()) {
            throw new RuntimeException("Użytkownik jest już członkiem tego projektu");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);

        ProjectMember saved = projectMemberRepository.save(member);

        // Wyślij wiadomość systemową
        String systemMessage = "👤 " + user.getUsername() + " dołączył do projektu jako " + getRoleDisplayName(role);
        messageService.sendSystemMessage(project, systemMessage);

        return saved;
    }

    // Usuń użytkownika z projektu
    @Transactional
    public void removeMemberFromProject(Project project, User user) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, user);
        if (memberOpt.isPresent()) {
            projectMemberRepository.delete(memberOpt.get());

            String systemMessage = "👤 " + user.getUsername() + " opuścił projekt";
            messageService.sendSystemMessage(project, systemMessage);
        }
    }

    // Usuń użytkownika ze wszystkich projektów - NAPRAWIONE
    @Transactional
    public void removeUserFromAllProjects(User user) {
        try {
            List<ProjectMember> userMemberships = projectMemberRepository.findByUser(user);

            System.out.println("Usuwanie użytkownika " + user.getUsername() + " z " + userMemberships.size() + " projektów");

            for (ProjectMember membership : userMemberships) {
                try {
                    Project project = membership.getProject();

                    // Usuń członkostwo
                    projectMemberRepository.delete(membership);

                    // Wyślij wiadomość systemową (jeśli się nie uda, kontynuuj)
                    String systemMessage = "👤 Użytkownik " + user.getUsername() + " został usunięty z projektu";
                    messageService.sendSystemMessage(project, systemMessage);

                } catch (Exception e) {
                    System.err.println("❌ Błąd podczas usuwania z projektu: " + e.getMessage());
                    // Kontynuuj z następnym projektem
                }
            }

            System.out.println("✅ Pomyślnie usunięto użytkownika ze wszystkich projektów");

        } catch (Exception e) {
            System.err.println("❌ Błąd usuwania użytkownika z projektów: " + e.getMessage());
            throw new RuntimeException("Nie udało się usunąć użytkownika z projektów: " + e.getMessage());
        }
    }

    // Pobierz projekty użytkownika
    public List<ProjectMember> getUserProjects(User user) {
        try {
            List<ProjectMember> memberships = projectMemberRepository.findByUser(user);

            // Filtruj tylko istniejące projekty
            return memberships.stream()
                    .filter(member -> {
                        try {
                            return member.getProject() != null && member.getProject().getId() != null;
                        } catch (Exception e) {
                            System.err.println("Uszkodzona relacja członkostwa: " + e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Błąd pobierania projektów użytkownika: " + e.getMessage());
            return List.of();
        }
    }

    // Pobierz członków projektu
    public List<ProjectMember> getProjectMembers(Project project) {
        return projectMemberRepository.findByProject(project);
    }

    // Pobierz konkretne członkostwo
    public Optional<ProjectMember> getProjectMember(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user);
    }

    // Zmień rolę użytkownika w projekcie
    @Transactional
    public void changeUserRole(Project project, User user, ProjectRole newRole) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, user);
        if (memberOpt.isPresent()) {
            ProjectMember member = memberOpt.get();
            ProjectRole oldRole = member.getRole();

            // Sprawdź czy to nie twórca projektu
            if (project.getCreatedBy().equals(user) && newRole != ProjectRole.ADMIN) {
                throw new RuntimeException("Nie można zmienić roli twórcy projektu");
            }

            member.setRole(newRole);
            projectMemberRepository.save(member);

            String systemMessage = "🔄 Rola użytkownika " + user.getUsername() +
                    " została zmieniona z " + getRoleDisplayName(oldRole) +
                    " na " + getRoleDisplayName(newRole);
            messageService.sendSystemMessage(project, systemMessage);
        }
    }

    // Sprawdź czy użytkownik jest członkiem projektu
    public boolean isUserMemberOfProject(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user).isPresent();
    }

    // Sprawdź czy użytkownik jest adminem projektu - NAPRAWIONE
    public boolean isProjectAdmin(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .map(member -> member.getRole() == ProjectRole.ADMIN)
                .orElse(false);
    }

    // Sprawdź czy użytkownik ma konkretną rolę w projekcie
    public boolean hasUserRoleInProject(Project project, User user, ProjectRole role) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, user);
        return memberOpt.isPresent() && memberOpt.get().getRole() == role;
    }

    // Metoda pomocnicza dla nazw ról
    private String getRoleDisplayName(ProjectRole role) {
        switch (role) {
            case ADMIN: return "Administrator";
            case MEMBER: return "Członek";
            case VIEWER: return "Obserwator";
            default: return role.toString();
        }
    }
}