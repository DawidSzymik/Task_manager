// src/main/java/com/example/demo/model/Message.java
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000, nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // WAŻNE: author może być null dla wiadomości systemowych
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = true)
    private User author;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isEdited = false;

    private LocalDateTime editedAt;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;

    // Konstruktory
    public Message() {}

    // Konstruktor dla zwykłych wiadomości
    public Message(String content, Project project, User author) {
        this.content = content;
        this.project = project;
        this.author = author;
        this.type = MessageType.TEXT;
    }

    // Konstruktor dla wiadomości systemowych
    public Message(String content, Project project) {
        this.content = content;
        this.project = project;
        this.author = null; // Wiadomości systemowe bez autora
        this.type = MessageType.SYSTEM;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        if (this.createdAt != null && !this.createdAt.equals(LocalDateTime.now())) {
            this.isEdited = true;
            this.editedAt = LocalDateTime.now();
        }
    }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    // Metody pomocnicze
    public String getFormattedTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getAuthorName() {
        if (author == null) {
            return "System";
        }
        return author.getUsername();
    }

    public boolean isSystemMessage() {
        return author == null || type == MessageType.SYSTEM;
    }

    public boolean canBeEditedBy(User user) {
        if (isSystemMessage()) return false;
        if (author == null) return false;
        return author.equals(user);
    }

    public boolean canBeDeletedBy(User user) {
        if (isSystemMessage()) return false;
        if (author == null) return false;
        return author.equals(user);
    }
}