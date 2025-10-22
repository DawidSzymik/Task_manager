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
import java.util.Set;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    public Comment createComment(Task task, User author, String text) {
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText(text);

        Comment saved = commentRepository.save(comment);

        // ✅ NOWE: Wyślij powiadomienia
        sendCommentNotifications(task, author, saved);

        return saved;
    }

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

    public Comment updateComment(Long commentId, String newText) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setText(newText);
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public List<Comment> getTaskComments(Task task) {
        return commentRepository.findByTaskOrderByCreatedAtDesc(task);
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
    }
}