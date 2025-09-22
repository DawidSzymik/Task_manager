// W TaskRepository.java dodaj te metody:

package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Znajdź zadania według projektu
    List<Task> findByProject(Project project);

    // Znajdź zadania przypisane do użytkownika - DODANE
    List<Task> findByAssignedTo(User user);

    // Inne metody które możesz mieć...
}