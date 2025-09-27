// src/main/java/com/example/demo/api/dto/request/LoginRequest.java
package com.example.demo.api.dto.request;

public class LoginRequest {

    private String username;
    private String password;

    // Constructors
    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Validation methods
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }

    public String getValidationError() {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        if (password == null || password.trim().isEmpty()) {
            return "Password is required";
        }
        if (username.length() > 50) {
            return "Username cannot exceed 50 characters";
        }
        if (password.length() > 100) {
            return "Password cannot exceed 100 characters";
        }
        return null;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "', password='***'}";
    }
}