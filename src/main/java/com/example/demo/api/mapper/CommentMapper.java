// src/main/java/com/example/demo/api/mapper/CommentMapper.java
package com.example.demo.api.mapper;

import com.example.demo.api.dto.request.CreateCommentRequest;
import com.example.demo.api.dto.request.UpdateCommentRequest;
import com.example.demo.api.dto.response.CommentDto;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.model.ProjectRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

    @Autowired
    private UserMapper userMapper;

    // Entity to DTO
    public CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        // Map author
        if (comment.getAuthor() != null) {
            dto.setAuthor(userMapper.toDto(comment.getAuthor()));
        }

        // Map task info
        if (comment.getTask() != null) {
            dto.setTaskId(comment.getTask().getId());
            dto.setTaskTitle(comment.getTask().getTitle());
        }

        return dto;
    }

    // Entity to DTO with permissions
    public CommentDto toDtoWithPermissions(Comment comment, User currentUser, ProjectRole userRole) {
        CommentDto dto = toDto(comment);

        // Set permissions
        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().equals(currentUser);
        boolean isAdmin = userRole == ProjectRole.ADMIN;

        dto.setCanEdit(isAuthor || isAdmin);
        dto.setCanDelete(isAuthor || isAdmin);

        return dto;
    }

    // Entity list to DTO list
    public List<CommentDto> toDto(List<Comment> comments) {
        return comments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Entity list to DTO list with permissions
    public List<CommentDto> toDtoWithPermissions(List<Comment> comments, User currentUser, ProjectRole userRole) {
        return comments.stream()
                .map(comment -> toDtoWithPermissions(comment, currentUser, userRole))
                .collect(Collectors.toList());
    }

    // CreateRequest to Entity
    public Comment toEntity(CreateCommentRequest request, User author) {
        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setAuthor(author);
        // Task will be set in the service layer
        return comment;
    }

    // Update entity from UpdateRequest
    public void updateEntity(Comment comment, UpdateCommentRequest request) {
        if (request.getText() != null) {
            comment.setText(request.getText());
        }
    }
}