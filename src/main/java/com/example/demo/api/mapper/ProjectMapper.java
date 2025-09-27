// src/main/java/com/example/demo/api/mapper/ProjectMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.request.CreateProjectRequest;
import com.example.demo.api.dto.request.UpdateProjectRequest;
import com.example.demo.api.dto.response.ProjectDto;
import com.example.demo.api.dto.response.ProjectMemberDto;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.model.Project;
import com.example.demo.model.ProjectMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProjectMapper {

    @Autowired
    private UserMapper userMapper;

    // Entity to DTO
    public ProjectDto toDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setDeadline(project.getDeadline());

        // Map creator
        if (project.getCreatedBy() != null) {
            dto.setCreatedBy(userMapper.toDto(project.getCreatedBy()));
        }

        return dto;
    }

    // Entity to DTO with members
    public ProjectDto toDtoWithMembers(Project project, List<ProjectMember> members) {
        ProjectDto dto = toDto(project);

        if (members != null) {
            List<ProjectMemberDto> memberDtos = members.stream()
                    .map(this::toMemberDto)
                    .collect(Collectors.toList());
            dto.setMembers(memberDtos);
        }

        return dto;
    }

    // Entity to DTO with stats
    public ProjectDto toDtoWithStats(Project project, int taskCount, int completedTaskCount) {
        ProjectDto dto = toDto(project);
        dto.setTaskCount(taskCount);
        dto.setCompletedTaskCount(completedTaskCount);

        // Calculate status based on tasks
        if (taskCount == 0) {
            dto.setStatus("planning");
        } else if (completedTaskCount == taskCount) {
            dto.setStatus("completed");
        } else {
            dto.setStatus("active");
        }

        return dto;
    }

    // ProjectMember to DTO
    public ProjectMemberDto toMemberDto(ProjectMember member) {
        ProjectMemberDto dto = new ProjectMemberDto();
        dto.setId(member.getId());
        dto.setRole(member.getRole().name());
        dto.setJoinedAt(member.getJoinedAt());

        if (member.getUser() != null) {
            dto.setUser(userMapper.toDto(member.getUser()));
        }

        return dto;
    }

    // Entity list to DTO list
    public List<ProjectDto> toDto(List<Project> projects) {
        return projects.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // CreateRequest to Entity
    public Project toEntity(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setDeadline(request.getDeadline());
        return project;
    }

    // Update entity from UpdateRequest
    public void updateEntity(Project project, UpdateProjectRequest request) {
        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getDeadline() != null) {
            project.setDeadline(request.getDeadline());
        }
    }
}