// src/main/java/com/example/demo/service/MessageService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Wysłanie nowej wiadomości
    @Transactional
    public Message sendMessage(Project project, User author, String content) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, author);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Użytkownik nie jest członkiem projektu");
        }

        ProjectRole userRole = memberOpt.get().getRole();
        if (userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Viewerzy nie mogą wysyłać wiadomości");
        }

        Message message = new Message(content, project, author);
        Message saved = messageRepository.save(message);

        // Wyślij powiadomienia do wszystkich członków projektu (oprócz autora)
        try {
            List<ProjectMember> allMembers = projectMemberRepository.findByProject(project);
            for (ProjectMember member : allMembers) {
                if (!member.getUser().equals(author)) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            member.getUser(),
                            "💬 Nowa wiadomość w czacie",
                            author.getUsername() + " napisał w projekcie \"" + project.getName() + "\": " +
                                    (content.length() > 100 ? content.substring(0, 100) + "..." : content),
                            NotificationType.NEW_MESSAGE,
                            saved.getId(),
                            "/projects/" + project.getId() + "/chat"
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd wysyłania powiadomień o wiadomości: " + e.getMessage());
        }

        return saved;
    }

    // Wysłanie wiadomości systemowej
    @Transactional
    public Message sendSystemMessage(Project project, String content) {
        Message message = new Message(content, project, null);
        message.setType(MessageType.SYSTEM);
        return messageRepository.save(message);
    }

    // Pobranie wszystkich wiadomości dla projektu
    public List<Message> getProjectMessages(Project project) {
        return messageRepository.findByProjectOrderByCreatedAtAsc(project);
    }

    // Pobranie ostatnich X wiadomości
    public List<Message> getRecentMessages(Project project, int limit) {
        List<Message> messages = messageRepository.findTop50ByProjectOrderByCreatedAtDesc(project);
        if (messages.size() > limit) {
            messages = messages.subList(0, limit);
        }
        Collections.reverse(messages);
        return messages;
    }

    // Pobranie wiadomości po określonej dacie
    public List<Message> getMessagesAfter(Project project, LocalDateTime after) {
        return messageRepository.findByProjectAndCreatedAtAfterOrderByCreatedAtAsc(project, after);
    }

    // Edycja wiadomości
    @Transactional
    public Message editMessage(Long messageId, User editor, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Wiadomość nie istnieje"));

        if (!message.canBeEditedBy(editor)) {
            throw new RuntimeException("Nie możesz edytować tej wiadomości");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    // Usuwanie wiadomości
    @Transactional
    public void deleteMessage(Long messageId, User deleter, Project project) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Wiadomość nie istnieje"));

        if (!message.getProject().equals(project)) {
            throw new RuntimeException("Wiadomość nie należy do tego projektu");
        }

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, deleter);
        ProjectRole userRole = memberOpt.map(ProjectMember::getRole)
                .orElseThrow(() -> new RuntimeException("Brak uprawnień"));

        if (!message.canBeDeletedBy(deleter, userRole)) {
            throw new RuntimeException("Nie możesz usunąć tej wiadomości");
        }

        messageRepository.delete(message);
    }

    // Wyszukiwanie wiadomości
    public List<Message> searchMessages(Project project, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return messageRepository.findByProjectAndContentContainingIgnoreCase(project, searchTerm.trim());
    }

    // Pobranie najnowszej wiadomości w projekcie
    public Optional<Message> getLatestMessage(Project project) {
        Pageable limit = PageRequest.of(0, 1);
        List<Message> messages = messageRepository.findLatestMessageInProject(project, limit);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    // Pobranie wiadomości po ID
    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    // Sprawdź czy użytkownik może wysyłać wiadomości w projekcie
    public boolean canSendMessages(Project project, User user) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, user);
        if (memberOpt.isEmpty()) {
            return false;
        }
        return memberOpt.get().getRole() != ProjectRole.VIEWER;
    }

    // Usunięcie wszystkich wiadomości projektu
    @Transactional
    public void deleteAllProjectMessages(Project project) {
        List<Message> messages = messageRepository.findByProjectOrderByCreatedAtAsc(project);
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }
    }

    // Event class dla powiadomień
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