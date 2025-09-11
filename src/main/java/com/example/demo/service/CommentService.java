// src/main/java/com/example/demo/service/CommentService.java - ZMIENIONY
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

    // ZMIENIONA - dodaj autora komentarza
    public void addCommentToTask(Long taskId, String text, String username) {
        Task task = taskRepo.findById(taskId).orElseThrow();
        User author = userRepo.findByUsername(username).orElseThrow();

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setText(text);
        comment.setAuthor(author);
        commentRepo.save(comment);
    }

    public List<Comment> getCommentsForTask(Long taskId) {
        return commentRepo.findByTaskId(taskId);
    }
}