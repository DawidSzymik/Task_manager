// src/main/java/com/example/demo/service/TaskProposalService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.TaskProposalRepository;
import com.example.demo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskProposalService {

    @Autowired
    private TaskProposalRepository proposalRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectMemberService memberService;

    public TaskProposal createProposal(Project project, User proposedBy,
                                       String title, String description, LocalDateTime deadline) {
        TaskProposal proposal = new TaskProposal();
        proposal.setProject(project);
        proposal.setProposedBy(proposedBy);
        proposal.setTitle(title);
        proposal.setDescription(description);
        proposal.setDeadline(deadline);

        TaskProposal saved = proposalRepository.save(proposal);

        // Powiadomienia dla adminów projektu
        List<ProjectMember> admins = memberService.getProjectMembers(project).stream()
                .filter(member -> member.getRole() == ProjectRole.ADMIN)
                .collect(Collectors.toList());

        for (ProjectMember admin : admins) {
            notificationService.createNotification(
                    admin.getUser(),
                    "Nowa propozycja zadania",
                    proposedBy.getUsername() + " zaproponował nowe zadanie: " + title,
                    NotificationType.TASK_PROPOSAL_PENDING,
                    saved.getId(),
                    "/projects/" + project.getId() + "/proposals"
            );
        }

        return saved;
    }

    public void approveProposal(Long proposalId, User reviewedBy) {
        TaskProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Propozycja nie istnieje"));

        // Stwórz zadanie na podstawie propozycji
        Task task = new Task();
        task.setTitle(proposal.getTitle());
        task.setDescription(proposal.getDescription());
        task.setDeadline(proposal.getDeadline());
        task.setProject(proposal.getProject());

        Task savedTask = taskRepository.save(task);

        // Zaktualizuj propozycję
        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedAt(LocalDateTime.now());
        proposalRepository.save(proposal);

        // Powiadomienie dla autora propozycji
        notificationService.createNotification(
                proposal.getProposedBy(),
                "Propozycja zatwierdzona",
                "Twoja propozycja zadania '" + proposal.getTitle() + "' została zatwierdzona",
                NotificationType.TASK_PROPOSAL_APPROVED,
                savedTask.getId(),
                "/tasks/view/" + savedTask.getId()
        );
    }

    public void rejectProposal(Long proposalId, User reviewedBy, String reason) {
        TaskProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Propozycja nie istnieje"));

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setRejectionReason(reason);
        proposalRepository.save(proposal);

        // Powiadomienie dla autora propozycji
        notificationService.createNotification(
                proposal.getProposedBy(),
                "Propozycja odrzucona",
                "Twoja propozycja zadania '" + proposal.getTitle() + "' została odrzucona. Powód: " + reason,
                NotificationType.TASK_PROPOSAL_REJECTED,
                proposal.getId(),
                "/projects/" + proposal.getProject().getId()
        );
    }

    // ✅ POPRAWKA: getUserProjects zwraca List<Project>, nie List<ProjectMember>
    public List<TaskProposal> getPendingProposalsForAdmin(User admin) {
        // Pobierz wszystkie projekty użytkownika
        List<Project> userProjects = memberService.getUserProjects(admin);

        // Filtruj tylko te projekty gdzie użytkownik jest adminem
        List<Project> adminProjects = userProjects.stream()
                .filter(project -> {
                    Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, admin);
                    return memberOpt.isPresent() && memberOpt.get().getRole() == ProjectRole.ADMIN;
                })
                .collect(Collectors.toList());

        // Zwróć wszystkie oczekujące propozycje dla tych projektów
        return proposalRepository.findByProjectInAndStatus(adminProjects, ProposalStatus.PENDING);
    }

    public List<TaskProposal> getProposalsByProject(Project project) {
        return proposalRepository.findByProject(project);
    }

    public List<TaskProposal> getUserProposals(User user) {
        return proposalRepository.findByProposedBy(user);
    }

    public Optional<TaskProposal> getProposalById(Long id) {
        return proposalRepository.findById(id);
    }

    @Transactional
    public void deleteProposalsForTask(Long taskId) {
        try {
            // TaskProposal nie ma bezpośredniego związku z Task
            // Propozycje są niezależne od zadań
            System.out.println("Propozycje zadań nie wymagają usuwania dla zadania ID: " + taskId);
        } catch (Exception e) {
            System.err.println("Błąd podczas sprawdzania propozycji dla zadania ID: " + taskId);
            e.printStackTrace();
        }
    }
}