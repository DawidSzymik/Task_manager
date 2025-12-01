package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // DODAJ TĘ METODĘ
    List<Project> findByCreatedBy(User createdBy);
    Optional<Project> findByName(String name);
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members " +
            "ORDER BY p.createdAt DESC")
    List<Project> findAllWithMembers();

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members pm " +
            "WHERE pm.user.id = :userId " +
            "ORDER BY p.createdAt DESC")
    List<Project> findByUserIdWithMembers(@Param("userId") Long userId);
}