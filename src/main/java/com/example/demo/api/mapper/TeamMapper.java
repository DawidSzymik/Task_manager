// src/main/java/com/example/demo/api/mapper/TeamMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.request.CreateTeamRequest;
import com.example.demo.api.dto.request.UpdateTeamRequest;
import com.example.demo.api.dto.response.TeamDto;
import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.model.SystemRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeamMapper {

    @Autowired
    private UserMapper userMapper;

    // Entity to DTO (basic)
    public TeamDto toDto(Team team) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setCreatedAt(team.getCreatedAt());

        // Map creator
        if (team.getCreatedBy() != null) {
            dto.setCreatedBy(userMapper.toDto(team.getCreatedBy()));
        }

        // Basic member count
        if (team.getMembers() != null) {
            dto.setMemberCount(team.getMembers().size());
        }

        return dto;
    }

    // Entity to DTO with members
    public TeamDto toDtoWithMembers(Team team) {
        TeamDto dto = toDto(team);

        // Map all members
        if (team.getMembers() != null) {
            List<com.example.demo.api.dto.response.UserDto> memberDtos = team.getMembers().stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
            dto.setMembers(memberDtos);
        }

        return dto;
    }

    // Entity to DTO with permissions
    public TeamDto toDtoWithPermissions(Team team, User currentUser) {
        TeamDto dto = toDto(team);

        // Set permissions
        boolean isCreator = team.getCreatedBy() != null && team.getCreatedBy().equals(currentUser);
        boolean isSuperAdmin = currentUser.getSystemRole() == SystemRole.SUPER_ADMIN;
        boolean isMember = team.getMembers() != null && team.getMembers().contains(currentUser);

        dto.setCanEdit(isCreator || isSuperAdmin);
        dto.setCanDelete(isCreator || isSuperAdmin);
        dto.setMember(isMember);

        return dto;
    }

    // ✅ NOWA METODA: Entity to DTO with members, permissions, and stats
    public TeamDto toDtoComplete(Team team, User currentUser) {
        TeamDto dto = toDtoWithMembers(team);

        // Set permissions
        boolean isCreator = team.getCreatedBy() != null && team.getCreatedBy().equals(currentUser);
        boolean isSuperAdmin = currentUser.getSystemRole() == SystemRole.SUPER_ADMIN;
        boolean isMember = team.getMembers() != null && team.getMembers().contains(currentUser);

        dto.setCanEdit(isCreator || isSuperAdmin);
        dto.setCanDelete(isCreator || isSuperAdmin);
        dto.setMember(isMember);

        return dto;
    }

    // ✅ NOWA METODA: Entity to DTO with stats (projectCount, taskCount)
    public TeamDto toDtoWithStats(Team team, User currentUser, int projectCount, int taskCount) {
        TeamDto dto = toDtoComplete(team, currentUser);
        dto.setProjectCount(projectCount);
        dto.setTaskCount(taskCount);
        return dto;
    }

    // Entity list to DTO list
    public List<TeamDto> toDto(List<Team> teams) {
        return teams.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Entity list to DTO list with permissions
    public List<TeamDto> toDtoWithPermissions(List<Team> teams, User currentUser) {
        return teams.stream()
                .map(team -> toDtoWithPermissions(team, currentUser))
                .collect(Collectors.toList());
    }

    // CreateRequest to Entity
    public Team toEntity(CreateTeamRequest request, User creator) {
        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setCreatedBy(creator);
        return team;
    }

    // Update entity from UpdateRequest
    public void updateEntity(Team team, UpdateTeamRequest request) {
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            team.setName(request.getName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
    }
}