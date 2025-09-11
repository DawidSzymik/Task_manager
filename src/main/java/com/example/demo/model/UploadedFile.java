// src/main/java/com/example/demo/model/UploadedFile.java - ZMIENIONY
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;

    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    @ManyToOne
    private Task task;

    // DODANE - autor uploadu i data
    @ManyToOne
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    // NOWE
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}