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
    private NotificationService notificationService;

    @Autowired
    private MessageService messageService;

    // Dodaj użytkownika do projektu (główna metoda)
    @Transactional
    public ProjectMember addMember(Project project, User user, ProjectRole role, User addedBy) {
        // Sprawdź czy użytkownik już jest członkiem
        boolean alreadyMember = project.getMembers().stream()
                .anyMatch(m -> m.getUser().equals(user));

        ProjectMember member = new ProjectMember(project, user, role);
        ProjectMember saved = projectMemberRepository.save(member);

        // ✅ Powiadom tylko jeśli to nowy członek
        if (!alreadyMember) {
            notificationService.createNotification(
                    user,
                    "🎯 Dodano Cię do projektu",
                    addedBy.getUsername() + " dodał Cię do projektu: \"" + project.getName() + "\" jako " + getRoleDisplayName(role),
                    NotificationType.PROJECT_MEMBER_ADDED,
                    project.getId(),
                    "/projects/" + project.getId()
            );
        }

        return saved;
    }

    // ✅ ALIAS dla addMember - używany w ProjectApiController
    @Transactional
    public ProjectMember addMemberToProject(Project project, User user, ProjectRole role) {
        return addMember(project, user, role, user); // addedBy = user (tymczasowo)
    }

    // Usuń użytkownika z projektu
    @Transactional
    public void removeMemberFromProject(Project project, User user) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, user);

        if (memberOpt.isPresent()) {
            ProjectMember member = memberOpt.get();
            projectMemberRepository.delete(member);

            // ✅ Powiadom użytkownika
            notificationService.createNotification(
                    user,
                    "❌ Usunięto z projektu",
                    "Zostałeś usunięty z projektu: \"" + project.getName() + "\"",
                    NotificationType.PROJECT_MEMBER_REMOVED,
                    project.getId(),
                    "/projects"
            );
        }
    }

    // Zmień rolę użytkownika w projekcie
    @Transactional
    public void updateMemberRole(Project project, User user, ProjectRole newRole, User changedBy) {
        ProjectMember member = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new RuntimeException("Członek projektu nie istnieje"));

        ProjectRole oldRole = member.getRole();
        member.setRole(newRole);
        projectMemberRepository.save(member);

        // ✅ Powiadom użytkownika
        notificationService.createNotification(
                user,
                "🔄 Zmiana roli w projekcie",
                changedBy.getUsername() + " zmienił Twoją rolę w projekcie \"" + project.getName() +
                        "\" z " + getRoleDisplayName(oldRole) + " na " + getRoleDisplayName(newRole),
                NotificationType.PROJECT_ROLE_CHANGED,
                project.getId(),
                "/projects/" + project.getId()
        );
    }

    // ✅ ALIAS dla updateMemberRole - używany w ProjectApiController
    @Transactional
    public void changeUserRole(Project project, User user, ProjectRole newRole) {
        updateMemberRole(project, user, newRole, user); // changedBy = user (tymczasowo)
    }

    // Pobierz członka projektu
    public Optional<ProjectMember> getProjectMember(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user);
    }

    // Pobierz wszystkich członków projektu
    public List<ProjectMember> getProjectMembers(Project project) {
        return projectMemberRepository.findByProject(project);
    }

    // Sprawdź czy użytkownik jest członkiem projektu
    public boolean isProjectMember(Project project, User user) {
        return projectMemberRepository.existsByProjectAndUser(project, user);
    }

    // Sprawdź czy użytkownik jest adminem projektu
    public boolean isProjectAdmin(Project project, User user) {
        Optional<ProjectMember> memberOpt = getProjectMember(project, user);
        return memberOpt.isPresent() && memberOpt.get().getRole() == ProjectRole.ADMIN;
    }

    // Sprawdź czy użytkownik może edytować projekt
    public boolean canEditProject(Project project, User user) {
        return isProjectAdmin(project, user) || project.getCreatedBy().equals(user);
    }

    // Pobierz rolę użytkownika w projekcie
    public ProjectRole getUserRole(Project project, User user) {
        return getProjectMember(project, user)
                .map(ProjectMember::getRole)
                .orElse(null);
    }

    // ✅ POPRAWKA: getUserProjects zwraca List<Project>, NIE List<ProjectMember>
    public List<Project> getUserProjects(User user) {
        List<ProjectMember> memberships = projectMemberRepository.findByUser(user);
        return memberships.stream()
                .map(ProjectMember::getProject)
                .distinct()
                .collect(Collectors.toList());
    }

    // Pobierz członków z określoną rolą
    public List<ProjectMember> getMembersByRole(Project project, ProjectRole role) {
        return projectMemberRepository.findByProjectAndRole(project, role);
    }

    // ✅ DODANA METODA - używana w różnych miejscach
    public boolean existsByProjectAndUser(Project project, User user) {
        return projectMemberRepository.existsByProjectAndUser(project, user);
    }

    // Helper: Wyświetlana nazwa roli
    private String getRoleDisplayName(ProjectRole role) {
        return switch (role) {
            case ADMIN -> "Administrator";
            case MEMBER -> "Członek";
            case VIEWER -> "Obserwator";
        };
    }
}