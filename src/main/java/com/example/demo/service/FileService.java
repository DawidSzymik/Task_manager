// src/main/java/com/example/demo/service/FileService.java
package com.example.demo.service;

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

    // ✅ ZAKTUALIZOWANA METODA - z powiadomieniami
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

            UploadedFile saved = fileRepository.save(uploadedFile);

            // ✅ WYSYŁANIE POWIADOMIEŃ
            try {
                String fileName = file.getOriginalFilename();

                // Powiadom wszystkich przypisanych użytkowników (oprócz osoby dodającej plik)
                Set<User> assignedUsers = task.getAssignedUsers();
                for (User assignedUser : assignedUsers) {
                    if (!assignedUser.equals(uploader)) {
                        notificationService.createNotification(
                                assignedUser,
                                "📎 Nowy plik w zadaniu",
                                uploader.getUsername() + " dodał plik \"" + fileName +
                                        "\" do zadania \"" + task.getTitle() + "\"",
                                NotificationType.TASK_FILE_UPLOADED,
                                task.getId(),
                                "/tasks/view/" + task.getId()
                        );
                    }
                }

                // Powiadom także twórcę zadania (jeśli nie jest przypisany i nie jest osobą dodającą)
                if (task.getCreatedBy() != null && !task.getCreatedBy().equals(uploader)) {
                    boolean creatorIsAssigned = assignedUsers.stream()
                            .anyMatch(u -> u.equals(task.getCreatedBy()));

                    if (!creatorIsAssigned) {
                        notificationService.createNotification(
                                task.getCreatedBy(),
                                "📎 Nowy plik w Twoim zadaniu",
                                uploader.getUsername() + " dodał plik \"" + fileName +
                                        "\" do zadania \"" + task.getTitle() + "\"",
                                NotificationType.TASK_FILE_UPLOADED,
                                task.getId(),
                                "/tasks/view/" + task.getId()
                        );
                    }
                }
            } catch (Exception e) {
                // Loguj błąd, ale nie przerywaj dodawania pliku
                System.err.println("❌ Błąd wysyłania powiadomienia o pliku: " + e.getMessage());
                e.printStackTrace();
            }

            return saved;

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
                .orElseThrow(() -> new RuntimeException("File with ID " + fileId + " not found"));
        fileRepository.delete(file);
    }

    // Usuń wszystkie pliki dla zadania
    @Transactional
    public void deleteFilesByTask(Task task) {
        List<UploadedFile> files = getFilesByTask(task);
        fileRepository.deleteAll(files);
    }
    // Pobierz pliki użytkownika
    public List<UploadedFile> getFilesByUser(User user) {
        return fileRepository.findByUploadedBy(user);
    }
}