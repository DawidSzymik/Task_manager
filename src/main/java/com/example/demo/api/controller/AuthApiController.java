// src/main/java/com/example/demo/api/controller/AuthApiController.java
package com.example.demo.api.controller;

import com.example.demo.api.dto.request.LoginRequest;
import com.example.demo.api.dto.request.RegisterRequest;
import com.example.demo.api.dto.response.AuthResponse;
import com.example.demo.api.dto.response.UserDto;
import com.example.demo.api.mapper.UserMapper;
import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthApiController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    public AuthApiController(UserService userService,
                             UserMapper userMapper,
                             AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
    }

    // POST /api/v1/auth/login - Login user
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            // Check if user exists and is active
            Optional<User> userOpt = userService.getUserByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                return createErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }

            User user = userOpt.get();
            if (!user.isActive()) {
                return createErrorResponse("Account is deactivated", HttpStatus.UNAUTHORIZED);
            }

            // Authenticate user
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            // Set authentication details
            authToken.setDetails(new WebAuthenticationDetails(httpRequest));

            Authentication authentication = authenticationManager.authenticate(authToken);

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Create session
            HttpSession session = httpRequest.getSession(true);
            String sessionId = session.getId();

            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userService.saveUser(user);

            // Create response
            UserDto userDto = userMapper.toDto(user);
            AuthResponse authResponse = AuthResponse.successWithSession(
                    userDto, sessionId, "Login successful"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("data", authResponse);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return createErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/auth/register - Register new user
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {

        try {
            // Validate request
            String validationError = request.getValidationError();
            if (validationError != null) {
                return createErrorResponse(validationError, HttpStatus.BAD_REQUEST);
            }

            // Check if username already exists
            if (userService.userExists(request.getUsername())) {
                return createErrorResponse("Username '" + request.getUsername() + "' already exists", HttpStatus.CONFLICT);
            }

            // Create new user
            User newUser = userService.createUser(request.getUsername(), request.getPassword());

            // Set additional fields
            newUser.setEmail(request.getEmail());
            newUser.setFullName(request.getFullName());
            newUser.setSystemRole(SystemRole.USER); // Default role

            User savedUser = userService.saveUser(newUser);

            // Create response
            UserDto userDto = userMapper.toDto(savedUser);
            AuthResponse authResponse = AuthResponse.success(userDto, "Registration successful");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("data", authResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Registration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/v1/auth/logout - Logout user
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {

        try {
            // Invalidate session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            // Clear security context
            SecurityContextHolder.clearContext();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Logout failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/auth/me - Get current user info
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
            }

            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserDto userDto = userMapper.toDto(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User info retrieved successfully");
            response.put("data", userDto);

            // Add session info
            HttpSession session = request.getSession(false);
            if (session != null) {
                response.put("sessionId", session.getId());
                response.put("sessionCreated", session.getCreationTime());
                response.put("sessionLastAccessed", session.getLastAccessedTime());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to get user info: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/auth/status - Check authentication status
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null &&
                    authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getPrincipal());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("authenticated", isAuthenticated);

            if (isAuthenticated) {
                response.put("username", authentication.getName());
                response.put("authorities", authentication.getAuthorities());

                HttpSession session = request.getSession(false);
                if (session != null) {
                    response.put("sessionId", session.getId());
                    response.put("sessionValid", true);
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Failed to check auth status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}