package com.example.demo.repository;

import com.example.demo.model.Message;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByProjectOrderByCreatedAtAsc(Project project);
    List<Message> findTop50ByProjectOrderByCreatedAtDesc(Project project);
    Page<Message> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);
    List<Message> findByProjectAndAuthorOrderByCreatedAtDesc(Project project, User author);
    List<Message> findByProjectAndCreatedAtAfterOrderByCreatedAtAsc(Project project, LocalDateTime after);

    @Query("SELECT m FROM Message m WHERE m.project = :project ORDER BY m.createdAt DESC")
    List<Message> findLatestMessageInProject(@Param("project") Project project, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.project = :project AND m.createdAt > :lastRead")
    long countUnreadMessages(@Param("project") Project project, @Param("lastRead") LocalDateTime lastRead);

    @Query("SELECT m FROM Message m WHERE m.project = :project AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.createdAt DESC")
    List<Message> findByProjectAndContentContainingIgnoreCase(@Param("project") Project project, @Param("searchTerm") String searchTerm);

    // NOWA METODA dla usuwania projektów
    @Modifying
    @Query("DELETE FROM Message m WHERE m.project = :project")
    void deleteByProject(@Param("project") Project project);
}
