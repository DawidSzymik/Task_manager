// src/main/java/com/example/demo/service/ProjectMemberService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Dodaj członka do projektu
    public ProjectMember addMemberToProject(Project project, User user, ProjectRole role) {
        if (memberRepository.existsByProjectAndUser(project, user)) {
            return memberRepository.findByProjectAndUser(project, user).orElse(null);
        }

        ProjectMember member = new ProjectMember(project, user, role);
        ProjectMember saved = memberRepository.save(member);

        try {
            // Wiadomość systemowa w czacie
            String roleText = getRoleDisplayName(role);
            String systemMessage = "👋 " + user.getUsername() + " dołączył do projektu jako " + roleText;
            eventPublisher.publishEvent(new SystemMessageEvent(project, systemMessage));

            // NOWE: Powiadomienie dla dodanego użytkownika
            eventPublisher.publishEvent(new NotificationEvent(
                    user,
                    "🎉 Dodano Cię do projektu",
                    "Zostałeś dodany do projektu \"" + project.getName() + "\" jako " + roleText,
                    NotificationType.PROJECT_MEMBER_ADDED,
                    project.getId(),
                    "/projects/" + project.getId()
            ));

        } catch (Exception e) {
            System.err.println("Błąd wysyłania eventów: " + e.getMessage());
        }

        return saved;
    }

    public void removeMemberFromProject(Project project, User user) {
        Optional<ProjectMember> member = memberRepository.findByProjectAndUser(project, user);
        if (member.isPresent()) {
            try {
                String systemMessage = "👋 " + user.getUsername() + " opuścił projekt";
                eventPublisher.publishEvent(new SystemMessageEvent(project, systemMessage));
            } catch (Exception e) {
                System.err.println("Błąd wysyłania eventu: " + e.getMessage());
            }
            memberRepository.delete(member.get());
        }
    }

    public void changeUserRole(Project project, User user, ProjectRole newRole, User changedBy) {
        Optional<ProjectMember> memberOpt = memberRepository.findByProjectAndUser(project, user);

        if (memberOpt.isPresent()) {
            ProjectMember member = memberOpt.get();
            ProjectRole oldRole = member.getRole();

            if (project.getCreatedBy().equals(user) && newRole != ProjectRole.ADMIN) {
                throw new RuntimeException("Nie można zmienić roli twórcy projektu");
            }

            member.setRole(newRole);
            memberRepository.save(member);

            if (!oldRole.equals(newRole)) {
                try {
                    String oldRoleText = getRoleDisplayName(oldRole);
                    String newRoleText = getRoleDisplayName(newRole);
                    String systemMessage = "🔧 " + changedBy.getUsername() + " zmienił rolę użytkownika " +
                            user.getUsername() + " z " + oldRoleText + " na " + newRoleText;
                    eventPublisher.publishEvent(new SystemMessageEvent(project, systemMessage));
                } catch (Exception e) {
                    System.err.println("Błąd wysyłania eventu: " + e.getMessage());
                }
            }
        }
    }

    public List<ProjectMember> getProjectMembers(Project project) {
        return memberRepository.findByProject(project);
    }

    public List<ProjectMember> getUserProjects(User user) {
        return memberRepository.findByUser(user);
    }

    public Optional<ProjectMember> getProjectMember(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user);
    }

    public boolean isProjectAdmin(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user)
                .map(member -> member.getRole() == ProjectRole.ADMIN)
                .orElse(false);
    }

    public boolean isProjectMember(Project project, User user) {
        return memberRepository.existsByProjectAndUser(project, user);
    }

    private String getRoleDisplayName(ProjectRole role) {
        switch (role) {
            case ADMIN: return "Administrator";
            case MEMBER: return "Członek";
            case VIEWER: return "Obserwator";
            default: return role.toString();
        }
    }

    // Event classes
    public static class SystemMessageEvent {
        private final Project project;
        private final String message;

        public SystemMessageEvent(Project project, String message) {
            this.project = project;
            this.message = message;
        }

        public Project getProject() { return project; }
        public String getMessage() { return message; }
    }

    // NOWA klasa eventu dla powiadomień
    public static class NotificationEvent {
        private final User user;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final Long relatedId;
        private final String actionUrl;

        public NotificationEvent(User user, String title, String message, NotificationType type, Long relatedId, String actionUrl) {
            this.user = user;
            this.title = title;
            this.message = message;
            this.type = type;
            this.relatedId = relatedId;
            this.actionUrl = actionUrl;
        }

        public User getUser() { return user; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public Long getRelatedId() { return relatedId; }
        public String getActionUrl() { return actionUrl; }
    }
}