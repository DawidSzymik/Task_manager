// src/main/java/com/example/demo/model/Comment.java - ZMIENIONY ajdnsjadjasn
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private Task task;

    // DODANE - autor komentarza
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    // NOWE
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
}