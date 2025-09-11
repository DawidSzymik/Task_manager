// src/main/java/com/example/demo/repository/TaskProposalRepository.java
package com.example.demo.repository;

import com.example.demo.model.TaskProposal;
import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskProposalRepository extends JpaRepository<TaskProposal, Long> {

    List<TaskProposal> findByProject(Project project);
    List<TaskProposal> findByProposedBy(User user);
    List<TaskProposal> findByProjectAndStatus(Project project, ProposalStatus status);
    List<TaskProposal> findByStatus(ProposalStatus status);

    // Propozycje oczekujące w projektach gdzie user jest adminem
    List<TaskProposal> findByProjectInAndStatus(List<Project> projects, ProposalStatus status);
}