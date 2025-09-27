// src/main/java/com/example/demo/api/dto/request/UpdateCommentRequest.java
package com.example.demo.api.dto.request;

import javax.validation.constraints.NotBlank;

public class UpdateCommentRequest {

    @NotBlank(message = "Comment text is required")
    private String text;

    public UpdateCommentRequest() {}

    public UpdateCommentRequest(String text) {
        this.text = text;
    }

    // Getters and setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // Validation helper
    public String getValidationError() {
        if (text == null || text.trim().isEmpty()) {
            return "Comment text is required";
        }
        if (text.length() > 1000) {
            return "Comment text cannot exceed 1000 characters";
        }
        return null;
    }

    @Override
    public String toString() {
        return "UpdateCommentRequest{" +
                "text='" + text + '\'' +
                '}';
    }
}