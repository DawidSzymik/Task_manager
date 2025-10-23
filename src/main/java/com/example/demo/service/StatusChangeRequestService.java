// src/main/java/com/example/demo/service/StatusChangeRequestService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.StatusChangeRequestRepository;
import com.example.demo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StatusChangeRequestService {

    @Autowired
    private StatusChangeRequestRepository requestRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectMemberService memberService;

    public StatusChangeRequest requestStatusChange(Task task, String newStatus, User requestedBy) {
        StatusChangeRequest request = new StatusChangeRequest();
        request.setTask(task);
        request.setCurrentStatus(task.getStatus());
        request.setRequestedStatus(newStatus);
        request.setRequestedBy(requestedBy);

        StatusChangeRequest saved = requestRepository.save(request);

        // Powiadomienia dla adminów projektu
        List<ProjectMember> admins = memberService.getProjectMembers(task.getProject()).stream()
                .filter(member -> member.getRole() == ProjectRole.ADMIN)
                .toList();

        for (ProjectMember admin : admins) {
            notificationService.createNotification(
                    admin.getUser(),
                    "Prośba o zmianę statusu",
                    requestedBy.getUsername() + " prosi o zmianę statusu zadania '" +
                            task.getTitle() + "' na " + newStatus,
                    NotificationType.STATUS_CHANGE_PENDING,
                    saved.getId(),
                    "/projects/" + task.getProject().getId() + "/status-requests"
            );
        }

        return saved;
    }

    public void approveStatusChange(Long requestId, User reviewedBy) {
        StatusChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Prośba nie istnieje"));

        Task task = request.getTask();
        task.setStatus(request.getRequestedStatus());
        taskRepository.save(task);

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Powiadom użytkownika który żądał
        notificationService.createNotification(
                request.getRequestedBy(),
                "✅ Zmiana statusu zatwierdzona",
                "Twoja prośba o zmianę statusu zadania \"" + task.getTitle() + "\" została zatwierdzona",
                NotificationType.STATUS_CHANGE_APPROVED,
                task.getId(),
                "/tasks/view/" + task.getId()
        );
    }

    public void rejectStatusChange(Long requestId, User reviewedBy, String reason) {
        StatusChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Prośba nie istnieje"));

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        requestRepository.save(request);

        // Powiadom użytkownika który żądał
        notificationService.createNotification(
                request.getRequestedBy(),
                "❌ Zmiana statusu odrzucona",
                "Twoja prośba o zmianę statusu zadania \"" + request.getTask().getTitle() +
                        "\" została odrzucona. Powód: " + reason,
                NotificationType.STATUS_CHANGE_REJECTED,
                request.getTask().getId(),
                "/tasks/view/" + request.getTask().getId()
        );
    }

    // ✅ POPRAWKA: getUserProjects zwraca List<Project>, więc najpierw pobieramy projekty
    // potem dla każdego projektu sprawdzamy czy user jest adminem
    public List<StatusChangeRequest> getPendingRequestsForAdmin(User admin) {
        // Pobierz wszystkie projekty użytkownika
        List<Project> userProjects = memberService.getUserProjects(admin);

        // Filtruj tylko te projekty gdzie użytkownik jest adminem
        List<Project> adminProjects = userProjects.stream()
                .filter(project -> {
                    Optional<ProjectMember> memberOpt = memberService.getProjectMember(project, admin);
                    return memberOpt.isPresent() && memberOpt.get().getRole() == ProjectRole.ADMIN;
                })
                .collect(Collectors.toList());

        // Pobierz wszystkie zadania z projektów gdzie użytkownik jest adminem
        List<Task> adminTasks = adminProjects.stream()
                .flatMap(project -> taskRepository.findByProject(project).stream())
                .collect(Collectors.toList());

        // Zwróć wszystkie oczekujące prośby dla tych zadań
        return requestRepository.findByTaskInAndStatus(adminTasks, RequestStatus.PENDING);
    }

    public List<StatusChangeRequest> getRequestsByTask(Task task) {
        return requestRepository.findByTask(task);
    }

    public Optional<StatusChangeRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    @Transactional
    public void deleteRequestsForTask(Long taskId) {
        try {
            List<StatusChangeRequest> requests = requestRepository.findAll().stream()
                    .filter(request -> request.getTask() != null && request.getTask().getId().equals(taskId))
                    .collect(Collectors.toList());

            if (!requests.isEmpty()) {
                requestRepository.deleteAll(requests);
                System.out.println("Usunięto " + requests.size() + " prośb o zmianę statusu dla zadania ID: " + taskId);
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania prośb o zmianę statusu dla zadania ID: " + taskId);
            e.printStackTrace();
        }
    }

    @Transactional
    public void deleteRequestsByTask(Task task) {
        try {
            List<StatusChangeRequest> requests = requestRepository.findByTask(task);
            if (!requests.isEmpty()) {
                requestRepository.deleteAll(requests);
                System.out.println("Usunięto " + requests.size() + " prośb o zmianę statusu dla zadania: " + task.getTitle());
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania prośb o zmianę statusu dla zadania: " + task.getTitle());
            e.printStackTrace();
        }
    }
}