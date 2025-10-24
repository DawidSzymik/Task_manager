// src/main/java/com/example/demo/service/CommentService.java
package com.example.demo.service;

import com.example.demo.model.Comment;
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

    // Podstawowa metoda pobierania komentarzy dla zadania
    public List<Comment> getCommentsByTask(Task task) {
        return commentRepository.findByTask(task);
    }

    // Komentarze posortowane chronologicznie (najnowsze najpierw)
    public List<Comment> getCommentsByTaskSorted(Task task) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task);
    }

    public Comment saveComment(Comment comment) {
        if (comment.getId() == null) {
            // Nowy komentarz
            comment.setCreatedAt(LocalDateTime.now());
        } else {
            // Aktualizacja komentarza
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

    // Metoda używana w TaskMapper i innych miejscach
    public List<Comment> getCommentsForTask(Task task) {
        return commentRepository.findByTask(task);
    }

    // ✅ ZAKTUALIZOWANA METODA - z powiadomieniami
    @Transactional
    public Comment addCommentToTask(Task task, String commentText, User author) {
        Comment comment = new Comment();
        comment.setText(commentText);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);

        // ✅ WYSYŁANIE POWIADOMIEŃ
        try {
            // Skróć treść komentarza dla powiadomienia (max 50 znaków)
            String shortText = commentText.length() > 50
                    ? commentText.substring(0, 50) + "..."
                    : commentText;

            // Powiadom wszystkich przypisanych użytkowników (oprócz autora komentarza)
            Set<User> assignedUsers = task.getAssignedUsers();
            for (User assignedUser : assignedUsers) {
                if (!assignedUser.equals(author)) {
                    notificationService.createNotification(
                            assignedUser,
                            "💬 Nowy komentarz w zadaniu",
                            author.getUsername() + " skomentował zadanie \"" + task.getTitle() + "\": " + shortText,
                            NotificationType.TASK_COMMENT_ADDED,
                            task.getId(),
                            "/tasks/view/" + task.getId()
                    );
                }
            }

            // Powiadom także twórcę zadania (jeśli nie jest przypisany i nie jest autorem komentarza)
            if (task.getCreatedBy() != null && !task.getCreatedBy().equals(author)) {
                boolean creatorIsAssigned = assignedUsers.stream()
                        .anyMatch(u -> u.equals(task.getCreatedBy()));

                if (!creatorIsAssigned) {
                    notificationService.createNotification(
                            task.getCreatedBy(),
                            "💬 Nowy komentarz w Twoim zadaniu",
                            author.getUsername() + " skomentował zadanie \"" + task.getTitle() + "\"",
                            NotificationType.TASK_COMMENT_ADDED,
                            task.getId(),
                            "/tasks/view/" + task.getId()
                    );
                }
            }
        } catch (Exception e) {
            // Loguj błąd, ale nie przerywaj dodawania komentarza
            System.err.println("❌ Błąd wysyłania powiadomienia o komentarzu: " + e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }

    // Dodatkowe metody użyteczne
    public long getCommentCountByTask(Task task) {
        return commentRepository.countByTask(task);
    }

    // Pobierz ostatnie komentarze z limitem
    public List<Comment> getRecentCommentsByTask(Task task, int limit) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task)
                .stream()
                .limit(limit)
                .toList();
    }
}