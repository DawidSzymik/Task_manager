// src/main/java/com/example/demo/service/FileService.java
package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.UploadedFile;
import com.example.demo.model.User;
import com.example.demo.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    // Pobierz plik po ID
    public Optional<UploadedFile> getFileById(Long fileId) {
        return fileRepository.findById(fileId);
    }

    // Pobierz pliki dla zadania
    public List<UploadedFile> getFilesByTask(Task task) {
        return fileRepository.findByTask(task);
    }

    // Policz pliki dla zadania
    public long getFileCountByTask(Task task) {
        return fileRepository.countByTask(task);
    }

    // Zapisz plik dla zadania
    @Transactional
    public UploadedFile storeFile(Task task, MultipartFile file, User uploader) {
        try {
            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setOriginalName(file.getOriginalFilename());
            uploadedFile.setContentType(file.getContentType());
            uploadedFile.setFileSize(file.getSize());
            uploadedFile.setData(file.getBytes());
            uploadedFile.setTask(task);
            uploadedFile.setUploadedBy(uploader);
            uploadedFile.setUploadedAt(LocalDateTime.now());

            return fileRepository.save(uploadedFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    // Stara metoda dla kompatybilności wstecznej
    @Transactional
    public void storeFileForTask(Long taskId, MultipartFile file, String username) {
        Task task = taskService.findById(taskId);
        User uploader = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        storeFile(task, file, uploader);
    }

    // Usuń plik
    @Transactional
    public void deleteFile(Long fileId) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        fileRepository.delete(file);
    }

    // Usuń wszystkie pliki dla zadania
    @Transactional
    public void deleteByTask(Task task) {
        fileRepository.deleteByTask(task);
    }

    // Pobierz pliki użytkownika
    public List<UploadedFile> getFilesByUser(User user) {
        return fileRepository.findByUploadedBy(user);
    }

    // Pobierz ostatnie pliki dla zadania z limitem
    public List<UploadedFile> getRecentFilesByTask(Task task, int limit) {
        return fileRepository.findByTaskOrderByUploadedAtDesc(task)
                .stream()
                .limit(limit)
                .toList();
    }

    // Sprawdź czy plik istnieje
    public boolean fileExists(Long fileId) {
        return fileRepository.existsById(fileId);
    }

    // Pobierz całkowity rozmiar plików dla zadania
    public long getTotalFileSizeByTask(Task task) {
        return fileRepository.findByTask(task).stream()
                .mapToLong(UploadedFile::getFileSize)
                .sum();
    }
}