// src/main/java/com/example/demo/api/dto/response/FileDto.java
package com.example.demo.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDto {

    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    // Related entities
    private UserDto uploadedBy;
    private Long taskId;
    private String taskTitle; // Optional - for context

    // Additional info
    private String downloadUrl;
    private boolean canDelete;
    private boolean isImage;
    private boolean isDocument;

    public FileDto() {}

    public FileDto(Long id, String originalName, Long fileSize) {
        this.id = id;
        this.originalName = originalName;
        this.fileSize = fileSize;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileSizeFormatted() {
        return fileSizeFormatted;
    }

    public void setFileSizeFormatted(String fileSizeFormatted) {
        this.fileSizeFormatted = fileSizeFormatted;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public UserDto getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UserDto uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public boolean isDocument() {
        return isDocument;
    }

    public void setDocument(boolean document) {
        isDocument = document;
    }

    @Override
    public String toString() {
        return "FileDto{" +
                "id=" + id +
                ", originalName='" + originalName + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedAt=" + uploadedAt +
                ", uploadedBy=" + (uploadedBy != null ? uploadedBy.getUsername() : null) +
                ", taskId=" + taskId +
                '}';
    }
}