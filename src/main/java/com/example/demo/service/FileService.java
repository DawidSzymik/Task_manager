// src/main/java/com/example/demo/service/FileService.java - ZMIENIONY
package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UploadedFileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import com.example.demo.model.UploadedFile;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    // ZMIENIONA - dodaj autora uploadu
    public void storeFileForTask(Long taskId, MultipartFile file, String username) {
        try {
            Task task = taskRepo.findById(taskId).orElseThrow();
            User uploadedBy = userRepo.findByUsername(username).orElseThrow();

            UploadedFile uf = new UploadedFile();
            uf.setTask(task);
            uf.setOriginalName(file.getOriginalFilename());
            uf.setContentType(file.getContentType());
            uf.setData(file.getBytes());
            uf.setUploadedBy(uploadedBy);

            fileRepo.save(uf);
            System.out.println("Zapisano plik w bazie danych: " + file.getOriginalFilename() + " przez: " + username);
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