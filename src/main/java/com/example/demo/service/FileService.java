// src/main/java/com/example/demo/service/FileService.java
package com.example.demo.service;

import com.example.demo.model.Notification;
import com.example.demo.model.NotificationType;
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
import java.util.Set;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    public Optional<UploadedFile> getFileById(Long fileId) {
        return fileRepository.findById(fileId);
    }

    public List<UploadedFile> getFilesByTask(Task task) {
        return fileRepository.findByTask(task);
    }

    public long getFileCountByTask(Task task) {
        return fileRepository.countByTask(task);
    }

    @Transactional
    public UploadedFile storeFile(Task task, MultipartFile file, User uploader) {
        try {
            System.out.println("\n========================================");
            System.out.println("🔵 START storeFile");
            System.out.println("Zadanie: " + task.getTitle() + " (ID: " + task.getId() + ")");
            System.out.println("Uploader: " + uploader.getUsername() + " (ID: " + uploader.getId() + ")");
            System.out.println("Plik: " + file.getOriginalFilename());

            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setOriginalName(file.getOriginalFilename());
            uploadedFile.setContentType(file.getContentType());
            uploadedFile.setFileSize(file.getSize());
            uploadedFile.setData(file.getBytes());
            uploadedFile.setTask(task);
            uploadedFile.setUploadedBy(uploader);
            uploadedFile.setUploadedAt(LocalDateTime.now());

            UploadedFile saved = fileRepository.save(uploadedFile);
            System.out.println("✅ Plik zapisany (ID: " + saved.getId() + ")");

            // WYSYŁANIE POWIADOMIEŃ
            try {
                System.out.println("\n🔔 Rozpoczynam wysyłanie powiadomień...");
                String fileName = file.getOriginalFilename();

                Set<User> assignedUsers = task.getAssignedUsers();
                System.out.println("📋 Liczba przypisanych użytkowników: " + (assignedUsers != null ? assignedUsers.size() : 0));

                if (assignedUsers == null || assignedUsers.isEmpty()) {
                    System.out.println("⚠️ BRAK przypisanych użytkowników - nie wysyłam powiadomień");
                } else {
                    int notificationsSent = 0;
                    for (User assignedUser : assignedUsers) {
                        System.out.println("\n  👤 Sprawdzam użytkownika: " + assignedUser.getUsername() + " (ID: " + assignedUser.getId() + ")");

                        if (assignedUser.equals(uploader)) {
                            System.out.println("  ⏭️ Pomijam - to uploader pliku");
                            continue;
                        }

                        System.out.println("  📤 Wysyłam powiadomienie...");
                        try {
                            Notification notification = notificationService.createNotification(
                                    assignedUser,
                                    "📎 Nowy plik w zadaniu",
                                    uploader.getUsername() + " dodał plik \"" + fileName +
                                            "\" do zadania \"" + task.getTitle() + "\"",
                                    NotificationType.TASK_FILE_UPLOADED,
                                    task.getId(),
                                    "/tasks/view/" + task.getId()
                            );
                            System.out.println("  ✅ Powiadomienie wysłane (ID: " + notification.getId() + ")");
                            notificationsSent++;
                        } catch (Exception e) {
                            System.err.println("  ❌ Błąd wysyłania powiadomienia: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    System.out.println("\n✅ Wysłano łącznie " + notificationsSent + " powiadomień");
                }

            } catch (Exception e) {
                System.err.println("❌ KRYTYCZNY BŁĄD w sekcji powiadomień: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("🔵 KONIEC storeFile");
            System.out.println("========================================\n");
            return saved;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void storeFileForTask(Long taskId, MultipartFile file, String username) {
        Task task = taskService.findById(taskId);
        User uploader = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        storeFile(task, file, uploader);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File with ID " + fileId + " not found"));
        fileRepository.delete(file);
    }

    @Transactional
    public void deleteFilesByTask(Task task) {
        List<UploadedFile> files = getFilesByTask(task);
        fileRepository.deleteAll(files);
    }

    public List<UploadedFile> getFilesByUser(User user) {
        return fileRepository.findByUploadedBy(user);
    }

    /**
     * Zapisz plik z Azure Blob URL
     */
    public UploadedFile saveFile(Task task, MultipartFile file, User user, String blobUrl) throws IOException {

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalName(file.getOriginalFilename());
        uploadedFile.setContentType(file.getContentType());
        uploadedFile.setFileSize(file.getSize()); // ✅ POPRAWIONE
        uploadedFile.setBlobUrl(blobUrl);
        // NIE ustawiamy setData() - pliki są w Azure!
        uploadedFile.setTask(task);
        uploadedFile.setUploadedBy(user);
        uploadedFile.setUploadedAt(java.time.LocalDateTime.now());

        return fileRepository.save(uploadedFile); // ✅ POPRAWIONE
    }
}