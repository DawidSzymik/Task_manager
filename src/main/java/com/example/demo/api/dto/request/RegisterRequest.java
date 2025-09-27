// src/main/java/com/example/demo/api/dto/request/RegisterRequest.java
package com.example.demo.api.dto.request;

public class RegisterRequest {

    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private String fullName;

    // Constructors
    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String confirmPassword, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.email = email;
        this.fullName = fullName;
    }

    // Validation methods
    public boolean isValid() {
        return getValidationError() == null;
    }

    public String getValidationError() {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        if (username.length() < 3) {
            return "Username must be at least 3 characters long";
        }
        if (username.length() > 20) {
            return "Username cannot exceed 20 characters";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Username can only contain letters, numbers, and underscores";
        }
        if (password == null || password.trim().isEmpty()) {
            return "Password is required";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        if (password.length() > 50) {
            return "Password cannot exceed 50 characters";
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            return "Password confirmation does not match";
        }
        if (email != null && !email.isEmpty() && !isValidEmail(email)) {
            return "Invalid email format";
        }
        if (fullName != null && fullName.length() > 100) {
            return "Full name cannot exceed 100 characters";
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".") &&
                email.length() >= 5 && email.length() <= 100;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        return "RegisterRequest{username='" + username + "', email='" + email + "', fullName='" + fullName + "'}";
    }
}