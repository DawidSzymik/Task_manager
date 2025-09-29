// src/main/java/com/example/demo/repository/TaskRepository.java
package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Znajdź zadania po projekcie
    List<Task> findByProject(Project project);

    // Znajdź zadania przypisane do użytkownika (single assignment)
    List<Task> findByAssignedTo(User user);

    // Znajdź zadania przypisane do użytkownika (many-to-many)
    List<Task> findByAssignedUsersContaining(User user);

    // Znajdź zadania utworzone przez użytkownika
    List<Task> findByCreatedBy(User user);

    // Policz zadania po statusie
    long countByStatus(String status);

    // Znajdź zadania po statusie
    List<Task> findByStatus(String status);

    // Znajdź zadania po priorytecie
    List<Task> findByPriority(String priority);

    // Znajdź zadania po projekcie i statusie
    List<Task> findByProjectAndStatus(Project project, String status);

    // Znajdź przeterminowane zadania
    @Query("SELECT t FROM Task t WHERE t.deadline < :date AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Task> findOverdueTasks(@Param("date") LocalDateTime date);

    // Znajdź zadania ukończone w okresie
    @Query("SELECT t FROM Task t WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :start AND :end")
    List<Task> findCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Znajdź zadania z deadline w przedziale
    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :start AND :end")
    List<Task> findByDeadlineBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Policz zadania w projekcie
    long countByProject(Project project);

    // Policz zadania przypisane do użytkownika
    long countByAssignedUsersContaining(User user);
}