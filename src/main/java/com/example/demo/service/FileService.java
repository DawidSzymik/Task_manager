// src/main/java/com/example/demo/service/FileService.java
package com.example.demo.service;

import com.example.demo.model.UploadedFile;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.NotificationType;
import com.example.demo.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepository;

    @Autowired
    private NotificationService notificationService;

    // ===== CREATE / SAVE =====

    public UploadedFile saveFile(MultipartFile file, Task task, User uploadedBy) throws IOException {
        // ✅ POPRAWKA: getOriginalFilename() jest metodą MultipartFile, nie UploadedFile
        UploadedFile uploadedFile = new UploadedFile(
                file.getOriginalFilename(),  // ✅ To jest poprawne - MultipartFile.getOriginalFilename()
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                task,
                uploadedBy
        );

        UploadedFile saved = fileRepository.save(uploadedFile);

        // ✅ Wyślij powiadomienia
        sendFileNotifications(task, uploadedBy, saved);

        return saved;
    }

    // Alias dla saveFile (używany w różnych miejscach)
    public UploadedFile storeFile(Task task, MultipartFile file, User uploadedBy) {
        try {
            return saveFile(file, task, uploadedBy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    // ===== READ =====

    // Główna metoda pobierania plików dla zadania
    public List<UploadedFile> getFilesByTask(Task task) {
        return fileRepository.findByTaskOrderByUploadedAtDesc(task);
    }

    // Pobierz pliki użytkownika
    public List<UploadedFile> getFilesByUser(User user) {
        return fileRepository.findByUploadedBy(user);
    }

    // Pobierz pojedynczy plik
    public Optional<UploadedFile> getFileById(Long id) {
        return fileRepository.findById(id);
    }

    // ===== COUNT =====

    // Policz pliki dla zadania
    public long getFileCountByTask(Task task) {
        return fileRepository.countByTask(task);
    }

    // Policz pliki użytkownika
    public long getFileCountByUser(User user) {
        return fileRepository.countByUploadedBy(user);
    }

    // ===== DELETE =====

    public void deleteFile(Long fileId) {
        fileRepository.deleteById(fileId);
    }

    // ===== NOTIFICATIONS =====

    private void sendFileNotifications(Task task, User uploadedBy, UploadedFile file) {
        Set<User> usersToNotify = new HashSet<>();

        // 1. Dodaj twórcę zadania (jeśli to nie osoba wgrywająca)
        if (task.getCreatedBy() != null && !task.getCreatedBy().equals(uploadedBy)) {
            usersToNotify.add(task.getCreatedBy());
        }

        // 2. Dodaj wszystkich przypisanych użytkowników (jeśli to nie osoba wgrywająca)
        if (task.getAssignedUsers() != null) {
            task.getAssignedUsers().stream()
                    .filter(user -> !user.equals(uploadedBy))
                    .forEach(usersToNotify::add);
        }

        // 3. Dodaj użytkownika przypisanego bezpośrednio (stary sposób)
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(uploadedBy)) {
            usersToNotify.add(task.getAssignedTo());
        }

        // Wyślij powiadomienia
        for (User user : usersToNotify) {
            notificationService.createNotification(
                    user,
                    "📎 Nowy plik w zadaniu",
                    uploadedBy.getUsername() + " dodał plik \"" + file.getOriginalName() + "\" do zadania: \"" + task.getTitle() + "\"",
                    NotificationType.TASK_FILE_UPLOADED,
                    task.getId(),
                    "/tasks/" + task.getId()
            );
        }
    }
}