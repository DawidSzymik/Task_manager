// src/main/java/com/example/demo/repository/ProjectRepository.java
package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Znajdź projekty gdzie użytkownik jest przypisany
    @Query("SELECT p FROM Project p JOIN p.assignedUsers u WHERE u = :user")
    List<Project> findByAssignedUser(@Param("user") User user);

    // Znajdź projekt po nazwie
    Project findByName(String name);
}