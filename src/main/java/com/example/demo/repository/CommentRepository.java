// src/main/java/com/example/demo/repository/CommentRepository.java
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

    // Znajdź komentarze dla konkretnego zadania (najnowsze najpierw)
    List<Comment> findByTaskOrderByCreatedAtDesc(Task task);

    // Znajdź komentarze dla konkretnego zadania (najstarsze najpierw)
    List<Comment> findByTaskOrderByCreatedAtAsc(Task task);

    // Podstawowa metoda znajdowania po zadaniu
    List<Comment> findByTask(Task task);

    // Policz komentarze dla zadania
    long countByTask(Task task);

    // Usuń wszystkie komentarze dla zadania
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.task = :task")
    void deleteByTask(@Param("task") Task task);

    // Znajdź komentarze użytkownika
    @Query("SELECT c FROM Comment c WHERE c.author.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findByAuthorId(@Param("userId") Long userId);

    // Podstawowa metoda znajdowania po autorze
    List<Comment> findByAuthor(User author);

    // Znajdź ostatnie komentarze dla zadania (z limitem)
    @Query("SELECT c FROM Comment c WHERE c.task = :task ORDER BY c.createdAt DESC")
    List<Comment> findRecentByTask(@Param("task") Task task);
}