package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTask(Task task);
    List<Comment> findByAuthor(User author);
    void deleteByTask(Task task);

    // DODAJ TĘ METODĘ
    Comment findByTaskId(Long taskId);
}