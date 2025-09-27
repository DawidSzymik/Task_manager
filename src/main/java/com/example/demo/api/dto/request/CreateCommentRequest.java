// src/main/java/com/example/demo/api/dto/request/CreateCommentRequest.java
package com.example.demo.api.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateCommentRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotBlank(message = "Comment text is required")
    private String text;

    public CreateCommentRequest() {}

    public CreateCommentRequest(Long taskId, String text) {
        this.taskId = taskId;
        this.text = text;
    }

    // Getters and setters
    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // Validation helper
    public String getValidationError() {
        if (taskId == null) {
            return "Task ID is required";
        }
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
        return "CreateCommentRequest{" +
                "taskId=" + taskId +
                ", text='" + text + '\'' +
                '}';
    }
}