// src/main/java/com/example/demo/repository/MessageRepository.java
package com.example.demo.repository;

import com.example.demo.model.Message;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Znajdź wszystkie wiadomości dla projektu (najnowsze pierwsze)
    List<Message> findByProjectOrderByCreatedAtAsc(Project project);

    // Znajdź ostatnie X wiadomości dla projektu
    List<Message> findTop50ByProjectOrderByCreatedAtDesc(Project project);

    // Znajdź wiadomości z paginacją
    Page<Message> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);

    // Znajdź wiadomości napisane przez użytkownika w projekcie
    List<Message> findByProjectAndAuthorOrderByCreatedAtDesc(Project project, User author);

    // Znajdź wiadomości po określonej dacie
    List<Message> findByProjectAndCreatedAtAfterOrderByCreatedAtAsc(Project project, LocalDateTime after);

    // Znajdź najnowszą wiadomość w projekcie
    @Query("SELECT m FROM Message m WHERE m.project = :project ORDER BY m.createdAt DESC")
    List<Message> findLatestMessageInProject(@Param("project") Project project, Pageable pageable);

    // Policz nieprzeczytane wiadomości (dla przyszłej funkcji)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.project = :project AND m.createdAt > :lastRead")
    long countUnreadMessages(@Param("project") Project project, @Param("lastRead") LocalDateTime lastRead);

    // Znajdź wiadomości zawierające tekst (search)
    @Query("SELECT m FROM Message m WHERE m.project = :project AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.createdAt DESC")
    List<Message> findByProjectAndContentContainingIgnoreCase(@Param("project") Project project, @Param("searchTerm") String searchTerm);
}