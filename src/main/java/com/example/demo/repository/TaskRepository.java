// src/main/java/com/example/demo/repository/TaskRepository.java
package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Znajdź zadania według projektu
    List<Task> findByProject(Project project);

    // Znajdź zadania przypisane do użytkownika (pojedyncze przypisanie)
    List<Task> findByAssignedTo(User user);

    // NOWA METODA - znajdź zadania gdzie użytkownik jest w kolekcji assignedUsers (Many-to-Many)
    @Query("SELECT t FROM Task t JOIN t.assignedUsers u WHERE u = :user")
    List<Task> findByAssignedUsersContaining(@Param("user") User user);

    // Sprawdź ile zadań ma użytkownik (wszystkie relacje)
    @Query("SELECT COUNT(t) FROM Task t WHERE :user MEMBER OF t.assignedUsers OR t.assignedTo = :user")
    long countTasksWithUser(@Param("user") User user);
}