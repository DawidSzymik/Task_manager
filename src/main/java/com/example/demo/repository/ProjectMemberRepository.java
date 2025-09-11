// src/main/java/com/example/demo/repository/ProjectMemberRepository.java
package com.example.demo.repository;

import com.example.demo.model.ProjectMember;
import com.example.demo.model.ProjectRole;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProject(Project project);
    List<ProjectMember> findByUser(User user);
    List<ProjectMember> findByProjectAndRole(Project project, ProjectRole role);

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user = :user AND pm.role = :role")
    List<ProjectMember> findByUserAndRole(@Param("user") User user, @Param("role") ProjectRole role);

    boolean existsByProjectAndUser(Project project, User user);
}