// src/main/java/com/example/demo/api/controller/AdminUserApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.CreateUserRequest;
import com.example.demo.api.dto.request.UpdateUserRequest;
import com.example.demo.api.dto.response.ApiResponse;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.api.mapper.UserMapper;
import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
// Tymczasowo usunięte: @PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    // GET /api/v1/admin/users - Get all users
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {

        List<User> users;

        if (search != null && !search.trim().isEmpty()) {
            // Proste wyszukiwanie po username i email
            users = userService.getAllUsers().stream()
                    .filter(user ->
                            user.getUsername().toLowerCase().contains(search.toLowerCase()) ||
                                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(search.toLowerCase()))
                    )
                    .collect(Collectors.toList());
        } else if (active != null) {
            users = active ? userRepository.findByIsActiveTrue() : userRepository.findByIsActiveFalse();
        } else {
            users = userService.getAllUsers();
        }

        List<UserDto> userDtos = userMapper.toDto(users);

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", userDtos)
        );
    }

    // GET /api/v1/admin/users/{id} - Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.getUserById(id);

        if (userOpt.isEmpty()) {
            throw new EntityNotFoundException("User with ID " + id + " not found");
        }

        UserDto userDto = userMapper.toDto(userOpt.get());

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userDto)
        );
    }

    // POST /api/v1/admin/users - Create new user
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Check if username already exists
        Optional<User> existingUser = userService.getUserByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' already exists");
        }

        User user = userService.createUserByAdmin(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName(),
                request.getSystemRole()
        );

        UserDto userDto = userMapper.toDto(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/admin/users/" + user.getId()))
                .body(ApiResponse.success("User created successfully", userDto));
    }

    // PUT /api/v1/admin/users/{id} - Update user
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        Optional<User> userOpt = userService.getUserById(id);

        if (userOpt.isEmpty()) {
            throw new EntityNotFoundException("User with ID " + id + " not found");
        }

        User user = userOpt.get();

        // Don't allow updating super admin role for now
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN &&
                request.getSystemRole() != null &&
                request.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot change super admin role");
        }

        User updatedUser = userService.updateUserByAdmin(
                id,
                request.getEmail(),
                request.getFullName(),
                request.getSystemRole(),
                request.getActive() != null ? request.getActive() : user.isActive()
        );

        UserDto userDto = userMapper.toDto(updatedUser);

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", userDto)
        );
    }

    // DELETE /api/v1/admin/users/{id} - Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userService.getUserById(id);

        if (userOpt.isEmpty()) {
            throw new EntityNotFoundException("User with ID " + id + " not found");
        }

        User user = userOpt.get();

        // Prevent deleting super admin
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot delete super admin user");
        }

        userService.deleteUserByAdmin(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully")
        );
    }

    // GET /api/v1/admin/users/stats - Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getUserStats() {
        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getActiveUserCount();

        var stats = new Object() {
            public final long total = totalUsers;
            public final long active = activeUsers;
            public final long inactive = totalUsers - activeUsers;
            public final double activePercentage = totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0;
        };

        return ResponseEntity.ok(
                ApiResponse.success("User statistics retrieved successfully", stats)
        );
    }
}