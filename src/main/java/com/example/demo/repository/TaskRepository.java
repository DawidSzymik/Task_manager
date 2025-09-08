package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTeam(Team team);
}
