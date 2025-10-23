// src/main/java/com/example/demo/repository/ProjectMemberRepository.java
package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.ProjectMember;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // Znajdź członków użytkownika
    List<ProjectMember> findByUser(User user);

    // Znajdź członków projektu
    List<ProjectMember> findByProject(Project project);

    // Znajdź konkretne członkostwo
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    // Znajdź członków z określoną rolą
    List<ProjectMember> findByProjectAndRole(Project project, ProjectRole role);

    // ✅ DODANA METODA - sprawdź czy użytkownik jest członkiem projektu
    boolean existsByProjectAndUser(Project project, User user);

    // METODY USUWANIA
    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.project = :project")
    void deleteByProject(@Param("project") Project project);

    void deleteByUser(User user);
}