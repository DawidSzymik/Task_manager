// src/main/java/com/example/demo/service/CommentService.java
package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.NotificationType;
import com.example.demo.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    // ===== CREATE =====

    public Comment createComment(Task task, User author, String text) {
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText(text);

        Comment saved = commentRepository.save(comment);

        // ✅ Wyślij powiadomienia
        sendCommentNotifications(task, author, saved);

        return saved;
    }

    // ===== SAVE =====

    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
    }

    // ===== READ =====

    // Główna metoda pobierania komentarzy dla zadania
    public List<Comment> getCommentsByTask(Task task) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task);
    }

    // Alias dla getCommentsByTask (używany w starym kodzie)
    public List<Comment> getTaskComments(Task task) {
        return getCommentsByTask(task);
    }

    // Pobierz pojedynczy komentarz
    public Optional<Comment> getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    // ===== COUNT =====

    // Policz komentarze dla zadania
    public long getCommentCountByTask(Task task) {
        return commentRepository.countByTask(task);
    }

    // ===== UPDATE =====

    public Comment updateComment(Long commentId, String newText) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setText(newText);
        return commentRepository.save(comment);
    }

    // ===== DELETE =====

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    // ===== LEGACY METHOD (dla starego kodu) =====

    public Comment addCommentToTask(Task task, String commentText, User currentUser) {
        return createComment(task, currentUser, commentText);
    }

    // ===== NOTIFICATIONS =====

    private void sendCommentNotifications(Task task, User author, Comment comment) {
        Set<User> usersToNotify = new HashSet<>();

        // 1. Dodaj twórcę zadania (jeśli to nie autor komentarza)
        if (task.getCreatedBy() != null && !task.getCreatedBy().equals(author)) {
            usersToNotify.add(task.getCreatedBy());
        }

        // 2. Dodaj wszystkich przypisanych użytkowników (jeśli to nie autor komentarza)
        if (task.getAssignedUsers() != null) {
            task.getAssignedUsers().stream()
                    .filter(user -> !user.equals(author))
                    .forEach(usersToNotify::add);
        }

        // 3. Dodaj użytkownika przypisanego bezpośrednio (stary sposób)
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(author)) {
            usersToNotify.add(task.getAssignedTo());
        }

        // Wyślij powiadomienia
        for (User user : usersToNotify) {
            notificationService.createNotification(
                    user,
                    "💬 Nowy komentarz w zadaniu",
                    author.getUsername() + " dodał komentarz do zadania: \"" + task.getTitle() + "\"",
                    NotificationType.TASK_COMMENT_ADDED,
                    task.getId(),
                    "/tasks/" + task.getId()
            );
        }
    }
}