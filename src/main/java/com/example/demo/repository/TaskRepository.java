// src/main/java/com/example/demo/repository/TaskRepository.java - ZMIENIONY
package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // ZMIANA: findByTeam → findByProject
    List<Task> findByProject(Project project);
}