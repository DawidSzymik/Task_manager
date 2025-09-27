// src/main/java/com/example/demo/api/mapper/UserMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.request.CreateUserRequest;
import com.example.demo.api.dto.request.UpdateUserRequest;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.model.User;
import com.example.demo.model.Team;
import com.example.demo.model.ProjectMember;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    // Entity to DTO
    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setSystemRole(user.getSystemRole().name());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());

        // Map team names
        if (user.getTeams() != null) {
            List<String> teamNames = user.getTeams().stream()
                    .map(Team::getName)
                    .collect(Collectors.toList());
            dto.setTeamNames(teamNames);
        }

        return dto;
    }

    // Entity list to DTO list
    public List<UserDto> toDto(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // CreateRequest to Entity
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        // Password will be encoded in service layer
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setSystemRole(request.getSystemRole());
        user.setActive(true); // New users are active by default
        return user;
    }

    // Update entity from UpdateRequest
    public void updateEntity(User user, UpdateUserRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getSystemRole() != null) {
            user.setSystemRole(request.getSystemRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
    }

    // Enhanced DTO with project count
    public UserDto toDtoWithStats(User user, int projectCount) {
        UserDto dto = toDto(user);
        dto.setProjectCount(projectCount);
        return dto;
    }
}