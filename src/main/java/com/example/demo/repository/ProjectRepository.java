package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // DODAJ TĘ METODĘ
    List<Project> findByCreatedBy(User createdBy);
    Optional<Project> findByName(String name);

}