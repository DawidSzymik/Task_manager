// src/main/java/com/example/demo/service/CommentService.java - DODANIE USUWANIA
package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    // Dodawanie komentarza
    public void addCommentToTask(Long taskId, String text, String username) {
        Task task = taskRepo.findById(taskId).orElseThrow();
        User author = userRepo.findByUsername(username).orElseThrow();

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setText(text);
        comment.setAuthor(author);
        commentRepo.save(comment);
    }

    // Pobieranie komentarzy dla zadania
    public List<Comment> getCommentsForTask(Long taskId) {
        return commentRepo.findByTaskId(taskId);
    }

    // NOWA METODA - Pobieranie komentarza po ID
    public Comment getCommentById(Long commentId) {
        return commentRepo.findById(commentId).orElse(null);
    }

    // NOWA METODA - Usuwanie komentarza
    public void deleteComment(Long commentId) {
        Comment comment = commentRepo.findById(commentId).orElse(null);
        if (comment != null) {
            commentRepo.delete(comment);
            System.out.println("Usunięto komentarz ID: " + commentId + " z zadania: " + comment.getTask().getTitle());
        } else {
            throw new RuntimeException("Komentarz o ID " + commentId + " nie istnieje");
        }
    }

    // NOWA METODA - Usuwanie wszystkich komentarzy dla zadania (wywoływane przy usuwaniu zadania)
    public void deleteCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepo.findByTaskId(taskId);
        if (!comments.isEmpty()) {
            commentRepo.deleteAll(comments);
            System.out.println("Usunięto " + comments.size() + " komentarzy dla zadania ID: " + taskId);
        }
    }
}