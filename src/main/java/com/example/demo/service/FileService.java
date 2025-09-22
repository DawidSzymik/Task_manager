// src/main/java/com/example/demo/service/FileService.java - Z POWIADOMIENIAMI
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UploadedFileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Zapisywanie pliku
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

            UploadedFile saved = fileRepo.save(uf);
            System.out.println("Zapisano plik w bazie danych: " + file.getOriginalFilename() + " przez: " + username);

            // NOWE: Wyślij powiadomienia do wszystkich przypisanych użytkowników (oprócz autora)
            try {
                Set<User> assignedUsers = task.getAssignedUsers();
                for (User assignedUser : assignedUsers) {
                    if (!assignedUser.equals(uploadedBy)) { // Nie wysyłaj powiadomienia autorowi uploadu
                        eventPublisher.publishEvent(new NotificationEvent(
                                assignedUser,
                                "📎 Nowy plik w zadaniu",
                                uploadedBy.getUsername() + " dodał plik \"" + file.getOriginalFilename() +
                                        "\" do zadania \"" + task.getTitle() + "\"",
                                NotificationType.TASK_FILE_UPLOADED,
                                task.getId(),
                                "/tasks/view/" + task.getId()
                        ));
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd wysyłania powiadomień o pliku: " + e.getMessage());
            }

        } catch (IOException e) {
            throw new RuntimeException("Błąd zapisu pliku", e);
        }
    }

    // Pobieranie plików dla zadania
    public List<UploadedFile> getFilesForTask(Long taskId) {
        return fileRepo.findByTaskId(taskId);
    }

    // Pobieranie pliku po ID
    public UploadedFile getFileById(Long id) {
        return fileRepo.findById(id).orElse(null);
    }

    // Usuwanie pliku
    public void deleteFile(Long fileId) {
        UploadedFile file = fileRepo.findById(fileId).orElse(null);
        if (file != null) {
            String fileName = file.getOriginalName();
            String taskTitle = file.getTask().getTitle();

            fileRepo.delete(file);
            System.out.println("Usunięto plik: " + fileName + " z zadania: " + taskTitle);
        } else {
            throw new RuntimeException("Plik o ID " + fileId + " nie istnieje");
        }
    }

    // Usuwanie wszystkich plików dla zadania (wywoływane przy usuwaniu zadania)
    public void deleteFilesForTask(Long taskId) {
        List<UploadedFile> files = fileRepo.findByTaskId(taskId);
        if (!files.isEmpty()) {
            fileRepo.deleteAll(files);
            System.out.println("Usunięto " + files.size() + " plików dla zadania ID: " + taskId);
        }
    }

    // Pobieranie rozmiaru pliku w bazie (do statystyk)
    public long getTotalFileSizeForTask(Long taskId) {
        List<UploadedFile> files = fileRepo.findByTaskId(taskId);
        return files.stream()
                .mapToLong(file -> file.getData() != null ? file.getData().length : 0)
                .sum();
    }

    // Event class dla powiadomień
    public static class NotificationEvent {
        private final User user;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final Long relatedId;
        private final String actionUrl;

        public NotificationEvent(User user, String title, String message, NotificationType type, Long relatedId, String actionUrl) {
            this.user = user;
            this.title = title;
            this.message = message;
            this.type = type;
            this.relatedId = relatedId;
            this.actionUrl = actionUrl;
        }

        public User getUser() { return user; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public Long getRelatedId() { return relatedId; }
        public String getActionUrl() { return actionUrl; }
    }
}