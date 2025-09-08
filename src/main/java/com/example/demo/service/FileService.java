package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.events.Comment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import org.springframework.core.io.Resource; // ✅ poprawny
import com.example.demo.model.UploadedFile;
import com.example.demo.model.Task;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.UploadedFileRepository;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepo;

    @Autowired
    private TaskRepository taskRepo;

    public void storeFileForTask(Long taskId, MultipartFile file) {
        try {
            Task task = taskRepo.findById(taskId).orElseThrow();

            UploadedFile uf = new UploadedFile();
            uf.setTask(task);
            uf.setOriginalName(file.getOriginalFilename());
            uf.setContentType(file.getContentType());
            uf.setData(file.getBytes()); // <--- najważniejsze

            fileRepo.save(uf);
            System.out.println("Zapisano plik w bazie danych: " + file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Błąd zapisu pliku", e);
        }
    }


    public List<UploadedFile> getFilesForTask(Long taskId) {
        return fileRepo.findByTaskId(taskId);
    }

    public UploadedFile getFileById(Long id) {
        return fileRepo.findById(id).orElse(null);
    }

}

