package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTask(Task task);
    List<Comment> findByAuthor(User author);

    // NOWA METODA dla usuwania zadań
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.task = :task")
    void deleteByTask(@Param("task") Task task);
}