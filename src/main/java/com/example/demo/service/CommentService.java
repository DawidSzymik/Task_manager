package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private TaskRepository taskRepo;

    public void addCommentToTask(Long taskId, String text) {
        Task task = taskRepo.findById(taskId).orElseThrow();
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setText(text);
        commentRepo.save(comment);
    }

    public List<Comment> getCommentsForTask(Long taskId) {
        return commentRepo.findByTaskId(taskId);
    }
}
