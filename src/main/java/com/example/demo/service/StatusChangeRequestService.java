// src/main/java/com/example/demo/service/StatusChangeRequestService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.StatusChangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StatusChangeRequestService {

    @Autowired
    private StatusChangeRequestRepository requestRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

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

        // Zmień status zadania
        Task task = request.getTask();
        task.setStatus(request.getRequestedStatus());
        taskService.saveTask(task);

        // Zaktualizuj prośbę
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Powiadomienie dla autora prośby
        notificationService.createNotification(
                request.getRequestedBy(),
                "Zmiana statusu zatwierdzona",
                "Status zadania '" + task.getTitle() + "' został zmieniony na " + request.getRequestedStatus(),
                NotificationType.STATUS_CHANGE_APPROVED,
                task.getId(),
                "/tasks/view/" + task.getId()
        );

        // Powiadomienia dla wszystkich przypisanych do zadania
        for (User assignedUser : task.getAssignedUsers()) {
            if (!assignedUser.equals(request.getRequestedBy())) {
                notificationService.createNotification(
                        assignedUser,
                        "Status zadania zmieniony",
                        "Status zadania '" + task.getTitle() + "' został zmieniony na " + request.getRequestedStatus(),
                        NotificationType.TASK_STATUS_CHANGED,
                        task.getId(),
                        "/tasks/view/" + task.getId()
                );
            }
        }
    }

    public void rejectStatusChange(Long requestId, User reviewedBy, String reason) {
        StatusChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Prośba nie istnieje"));

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        requestRepository.save(request);

        // Powiadomienie dla autora prośby
        notificationService.createNotification(
                request.getRequestedBy(),
                "Zmiana statusu odrzucona",
                "Prośba o zmianę statusu zadania '" + request.getTask().getTitle() +
                        "' została odrzucona. Powód: " + reason,
                NotificationType.STATUS_CHANGE_REJECTED,
                request.getTask().getId(),
                "/tasks/view/" + request.getTask().getId()
        );
    }

    public List<StatusChangeRequest> getPendingRequestsForAdmin(User admin) {
        List<ProjectMember> adminMemberships = memberService.getUserProjects(admin).stream()
                .filter(member -> member.getRole() == ProjectRole.ADMIN)
                .toList();

        List<Task> adminTasks = adminMemberships.stream()
                .flatMap(member -> taskService.getTasksByProject(member.getProject()).stream())
                .toList();

        return requestRepository.findByTaskInAndStatus(adminTasks, RequestStatus.PENDING);
    }

    public List<StatusChangeRequest> getRequestsByTask(Task task) {
        return requestRepository.findByTask(task);
    }

    public Optional<StatusChangeRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }
}