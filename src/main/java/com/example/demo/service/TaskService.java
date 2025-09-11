// src/main/java/com/example/demo/service/TaskService.java - ZMIENIONY
package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.Project;

import java.util.List;
import java.util.Optional;

public interface TaskService {
    Task saveTask(Task task);
    List<Task> getTasksByProject(Project project); // ZMIANA: Team → Project
    List<Task> getAllTasks();

    Optional<Task> getTaskById(Long id);
    void deleteTask(Long id);

    Task findById(Long id);
    List<Task> findAllByProject(Project project); // ZMIANA: Team → Project
}