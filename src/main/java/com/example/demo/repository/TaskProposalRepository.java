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
    List<TaskProposal> findByProjectInAndStatus(List<Project> projects, ProposalStatus status);
}