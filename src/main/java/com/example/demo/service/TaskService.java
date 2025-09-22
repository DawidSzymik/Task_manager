// src/main/java/com/example/demo/service/TaskService.java
package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    // Podstawowe operacje CRUD
    Task saveTask(Task task);
    Optional<Task> getTaskById(Long id);
    Task findById(Long id); // Wrapper który rzuca wyjątek jeśli nie znajdzie
    List<Task> getAllTasks();
    void deleteTask(Long id);

    // Operacje na projektach
    List<Task> getTasksByProject(Project project);
    List<Task> findAllByProject(Project project);

    // Operacje na użytkownikach - DODANE
    List<Task> findByAssignedTo(User user);
    void unassignUserFromAllTasks(User user);

    // Operacje masowe
    void deleteAllTasksForProject(Long projectId);
}