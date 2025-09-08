package com.example.demo.model;

import javax.persistence.*;

@Entity
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;

    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB") // dla MySQL
    private byte[] data;

    @ManyToOne
    private Task task;

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
}
