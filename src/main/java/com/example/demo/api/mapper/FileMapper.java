// src/main/java/com/example/demo/api/mapper/FileMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.response.FileDto;
import com.example.demo.model.UploadedFile;
import com.example.demo.model.User;
import com.example.demo.model.ProjectRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileMapper {

    @Autowired
    private UserMapper userMapper;

    // Entity to DTO
    public FileDto toDto(UploadedFile file) {
        FileDto dto = new FileDto();
        dto.setId(file.getId());
        dto.setOriginalName(file.getOriginalName());
        dto.setContentType(file.getContentType());
        dto.setFileSize(file.getFileSize());
        dto.setFileSizeFormatted(formatFileSize(file.getFileSize()));
        dto.setUploadedAt(file.getUploadedAt());

        // Map uploader
        if (file.getUploadedBy() != null) {
            dto.setUploadedBy(userMapper.toDto(file.getUploadedBy()));
        }

        // Map task info
        if (file.getTask() != null) {
            dto.setTaskId(file.getTask().getId());
            dto.setTaskTitle(file.getTask().getTitle());
        }

        // Set download URL
        dto.setDownloadUrl("/api/v1/files/" + file.getId() + "/download");

        // Set file type flags
        dto.setImage(isImageFile(file.getContentType()));
        dto.setDocument(isDocumentFile(file.getContentType()));

        return dto;
    }

    // Entity to DTO with permissions
    public FileDto toDtoWithPermissions(UploadedFile file, User currentUser, ProjectRole userRole) {
        FileDto dto = toDto(file);

        // Set permissions
        boolean isUploader = file.getUploadedBy() != null && file.getUploadedBy().equals(currentUser);
        boolean isAdmin = userRole == ProjectRole.ADMIN;

        dto.setCanDelete(isUploader || isAdmin);

        return dto;
    }

    // Entity list to DTO list
    public List<FileDto> toDto(List<UploadedFile> files) {
        return files.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Entity list to DTO list with permissions
    public List<FileDto> toDtoWithPermissions(List<UploadedFile> files, User currentUser, ProjectRole userRole) {
        return files.stream()
                .map(file -> toDtoWithPermissions(file, currentUser, userRole))
                .collect(Collectors.toList());
    }

    // Helper methods
    private String formatFileSize(Long bytes) {
        if (bytes == null) return "0 B";

        long size = bytes;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private boolean isImageFile(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("image/");
    }

    private boolean isDocumentFile(String contentType) {
        if (contentType == null) return false;
        return contentType.contains("pdf") ||
                contentType.contains("document") ||
                contentType.contains("word") ||
                contentType.contains("excel") ||
                contentType.contains("spreadsheet") ||
                contentType.contains("text");
    }
}