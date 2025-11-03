// src/main/java/com/example/demo/service/CommentService.java
package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Notification;
import com.example.demo.model.NotificationType;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Comment> getCommentsByTask(Task task) {
        return commentRepository.findByTask(task);
    }

    public List<Comment> getCommentsByTaskSorted(Task task) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task);
    }

    public Comment saveComment(Comment comment) {
        if (comment.getId() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        } else {
            comment.setUpdatedAt(LocalDateTime.now());
        }
        return commentRepository.save(comment);
    }

    public Optional<Comment> getCommentById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment with ID " + commentId + " not found"));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findById(commentId);
        commentRepository.delete(comment);
    }

    @Transactional
    public void deleteByTask(Task task) {
        commentRepository.deleteByTask(task);
    }

    public List<Comment> getCommentsForTask(Task task) {
        return commentRepository.findByTask(task);
    }

    @Transactional
    public Comment addCommentToTask(Task task, String commentText, User author) {
        System.out.println("\n========================================");
        System.out.println("🔵 START addCommentToTask");
        System.out.println("Zadanie: " + task.getTitle() + " (ID: " + task.getId() + ")");
        System.out.println("Autor komentarza: " + author.getUsername() + " (ID: " + author.getId() + ")");
        System.out.println("Treść: " + commentText);

        Comment comment = new Comment();
        comment.setText(commentText);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        System.out.println("✅ Komentarz zapisany (ID: " + saved.getId() + ")");

        // WYSYŁANIE POWIADOMIEŃ
        try {
            System.out.println("\n🔔 Rozpoczynam wysyłanie powiadomień...");

            String shortText = commentText.length() > 50
                    ? commentText.substring(0, 50) + "..."
                    : commentText;

            Set<User> assignedUsers = task.getAssignedUsers();
            System.out.println("📋 Liczba przypisanych użytkowników: " + (assignedUsers != null ? assignedUsers.size() : 0));

            if (assignedUsers == null || assignedUsers.isEmpty()) {
                System.out.println("⚠️ BRAK przypisanych użytkowników - nie wysyłam powiadomień");
            } else {
                int notificationsSent = 0;
                for (User assignedUser : assignedUsers) {
                    System.out.println("\n  👤 Sprawdzam użytkownika: " + assignedUser.getUsername() + " (ID: " + assignedUser.getId() + ")");

                    if (assignedUser.equals(author)) {
                        System.out.println("  ⏭️ Pomijam - to autor komentarza");
                        continue;
                    }

                    System.out.println("  📤 Wysyłam powiadomienie...");
                    try {
                        Notification notification = notificationService.createNotification(
                                assignedUser,
                                "💬 Nowy komentarz w zadaniu",
                                author.getUsername() + " skomentował zadanie \"" + task.getTitle() + "\": " + shortText,
                                NotificationType.TASK_COMMENT_ADDED,
                                task.getId(),
                                "/tasks/view/" + task.getId()
                        );
                        System.out.println("  ✅ Powiadomienie wysłane (ID: " + notification.getId() + ")");
                        notificationsSent++;
                    } catch (Exception e) {
                        System.err.println("  ❌ Błąd wysyłania powiadomienia: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                System.out.println("\n✅ Wysłano łącznie " + notificationsSent + " powiadomień");
            }

        } catch (Exception e) {
            System.err.println("❌ KRYTYCZNY BŁĄD w sekcji powiadomień: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🔵 KONIEC addCommentToTask");
        System.out.println("========================================\n");
        return saved;
    }

    public long getCommentCountByTask(Task task) {
        return commentRepository.countByTask(task);
    }

    public List<Comment> getRecentCommentsByTask(Task task, int limit) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task)
                .stream()
                .limit(limit)
                .toList();
    }
}