// src/main/java/com/example/demo/service/CommentService.java - Z POWIADOMIENIAMI
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Dodawanie komentarza
    public void addCommentToTask(Long taskId, String text, String username) {
        Task task = taskRepo.findById(taskId).orElseThrow();
        User author = userRepo.findByUsername(username).orElseThrow();

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setText(text);
        comment.setAuthor(author);
        Comment saved = commentRepo.save(comment);

        // NOWE: Wyślij powiadomienia do wszystkich przypisanych użytkowników (oprócz autora komentarza)
        try {
            Set<User> assignedUsers = task.getAssignedUsers();
            for (User assignedUser : assignedUsers) {
                if (!assignedUser.equals(author)) { // Nie wysyłaj powiadomienia autorowi komentarza
                    eventPublisher.publishEvent(new NotificationEvent(
                            assignedUser,
                            "💬 Nowy komentarz w zadaniu",
                            author.getUsername() + " dodał komentarz do zadania \"" + task.getTitle() + "\": " +
                                    (text.length() > 100 ? text.substring(0, 100) + "..." : text),
                            NotificationType.TASK_COMMENT_ADDED,
                            task.getId(),
                            "/tasks/view/" + task.getId()
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd wysyłania powiadomień o komentarzu: " + e.getMessage());
        }
    }

    // Pobieranie komentarzy dla zadania
    public List<Comment> getCommentsForTask(Long taskId) {
        return commentRepo.findByTaskId(taskId);
    }

    // Pobieranie komentarza po ID
    public Comment getCommentById(Long commentId) {
        return commentRepo.findById(commentId).orElse(null);
    }

    // Usuwanie komentarza
    public void deleteComment(Long commentId) {
        Comment comment = commentRepo.findById(commentId).orElse(null);
        if (comment != null) {
            commentRepo.delete(comment);
            System.out.println("Usunięto komentarz ID: " + commentId + " z zadania: " + comment.getTask().getTitle());
        } else {
            throw new RuntimeException("Komentarz o ID " + commentId + " nie istnieje");
        }
    }

    // Usuwanie wszystkich komentarzy dla zadania (wywoływane przy usuwaniu zadania)
    public void deleteCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepo.findByTaskId(taskId);
        if (!comments.isEmpty()) {
            commentRepo.deleteAll(comments);
            System.out.println("Usunięto " + comments.size() + " komentarzy dla zadania ID: " + taskId);
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