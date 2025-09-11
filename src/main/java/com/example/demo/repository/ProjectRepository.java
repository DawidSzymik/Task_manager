// src/main/java/com/example/demo/repository/ProjectRepository.java - POPRAWIONY
package com.example.demo.repository;

import com.example.demo.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Znajdź projekt po nazwie
    Project findByName(String name);

    // USUŃ TĘ METODĘ - nie działa już z nowym modelem
    // List<Project> findByAssignedUser(@Param("user") User user);

    // Teraz używamy ProjectMemberRepository do znajdowania projektów użytkownika
}