// src/main/java/com/example/demo/api/dto/response/AuthResponse.java
package com.example.demo.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token; // For future JWT implementation
    private String sessionId; // Current session-based auth
    private UserDto user;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt; // For future JWT implementation

    // Constructors
    public AuthResponse() {
        this.loginTime = LocalDateTime.now();
    }

    public AuthResponse(UserDto user, String message) {
        this();
        this.user = user;
        this.message = message;
    }

    public AuthResponse(UserDto user, String sessionId, String message) {
        this();
        this.user = user;
        this.sessionId = sessionId;
        this.message = message;
    }

    // Static factory methods
    public static AuthResponse success(UserDto user, String message) {
        return new AuthResponse(user, message);
    }

    public static AuthResponse successWithSession(UserDto user, String sessionId, String message) {
        return new AuthResponse(user, sessionId, message);
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}