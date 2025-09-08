package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.Team;

import java.util.List;
import java.util.Optional;

public interface TaskService {
    Task saveTask(Task task);
    List<Task> getTasksByTeam(Team team);
    List<Task> getAllTasks();

    Optional<Task> getTaskById(Long id); // Już masz 👍
    void deleteTask(Long id);

    Task findById(Long id); // DODAJ
    List<Task> findAllByTeam(Team team); // DODAJ
}
