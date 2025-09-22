// src/main/java/com/example/demo/service/ProjectMemberService.java - NAPRAWIONY
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

            // Powiadomienie dla dodanego użytkownika
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

    // NAPRAWIONA METODA - Usuń użytkownika ze wszystkich projektów (dla super admina)
    @Transactional
    public void removeUserFromAllProjects(User user) {
        try {
            List<ProjectMember> userMemberships = memberRepository.findByUser(user);

            if (!userMemberships.isEmpty()) {
                System.out.println("Usuwam użytkownika " + user.getUsername() + " z " + userMemberships.size() + " projektów");

                // BEZPIECZNE usuwanie - najpierw usuń członkostwa, POTEM wyślij wiadomości
                for (ProjectMember membership : userMemberships) {
                    try {
                        memberRepository.delete(membership);
                        System.out.println("✅ Usunięto członkostwo w projekcie: " + membership.getProject().getName());
                    } catch (Exception e) {
                        System.err.println("❌ Błąd usuwania członkostwa: " + e.getMessage());
                    }
                }

                // DOPIERO TERAZ wyślij wiadomości systemowe (po usunięciu członkostw)
                for (ProjectMember membership : userMemberships) {
                    try {
                        Project project = membership.getProject();
                        if (project != null && project.getId() != null) {
                            String systemMessage = "🔴 " + user.getUsername() + " został usunięty z projektu przez administratora systemu";
                            // Nie blokuj całej transakcji jeśli nie uda się wysłać wiadomości
                            try {
                                eventPublisher.publishEvent(new SystemMessageEvent(project, systemMessage));
                            } catch (Exception msgError) {
                                System.err.println("⚠️  Nie udało się wysłać wiadomości systemowej dla projektu " + project.getName() + ": " + msgError.getMessage());
                                // Kontynuuj mimo błędu
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Błąd podczas przetwarzania projektu: " + e.getMessage());
                    }
                }

                System.out.println("✅ Pomyślnie usunięto użytkownika ze wszystkich projektów");
            }
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas usuwania użytkownika z projektów: " + e.getMessage());
            e.printStackTrace();
            // Nie rzucaj wyjątku - pozwól na kontynuację
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
        try {
            List<ProjectMember> memberships = memberRepository.findByUser(user);

            // NAPRAWKA - Filtruj tylko istniejące projekty
            return memberships.stream()
                    .filter(member -> {
                        try {
                            // Sprawdź czy projekt jeszcze istnieje
                            return member.getProject() != null && member.getProject().getId() != null;
                        } catch (Exception e) {
                            System.err.println("Uszkodzona relacja członkostwa dla użytkownika " + user.getUsername() + ": " + e.getMessage());

                            // Usuń uszkodzoną relację
                            try {
                                memberRepository.delete(member);
                            } catch (Exception deleteEx) {
                                System.err.println("Nie udało się usunąć uszkodzonej relacji: " + deleteEx.getMessage());
                            }

                            return false;
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Błąd pobierania projektów użytkownika " + user.getUsername() + ": " + e.getMessage());
            return List.of(); // Zwróć pustą listę w przypadku błędu
        }
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