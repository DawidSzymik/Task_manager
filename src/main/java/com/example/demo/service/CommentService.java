// src/main/java/com/example/demo/service/CommentService.java
package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

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

    // Metoda do dodawania komentarza do zadania
    public Comment addCommentToTask(Task task, String commentText, User author) {
        Comment comment = new Comment();
        comment.setText(commentText);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
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