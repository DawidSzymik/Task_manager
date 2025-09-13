// src/main/java/com/example/demo/service/FileService.java - DODANIE USUWANIA
package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.UploadedFile;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UploadedFileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private UploadedFileRepository fileRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

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

            fileRepo.save(uf);
            System.out.println("Zapisano plik w bazie danych: " + file.getOriginalFilename() + " przez: " + username);
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

    // NOWA METODA - Usuwanie pliku
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

    // NOWA METODA - Usuwanie wszystkich plików dla zadania (wywoływane przy usuwaniu zadania)
    public void deleteFilesForTask(Long taskId) {
        List<UploadedFile> files = fileRepo.findByTaskId(taskId);
        if (!files.isEmpty()) {
            fileRepo.deleteAll(files);
            System.out.println("Usunięto " + files.size() + " plików dla zadania ID: " + taskId);
        }
    }

    // NOWA METODA - Pobieranie rozmiaru pliku w bazie (do statystyk)
    public long getTotalFileSizeForTask(Long taskId) {
        List<UploadedFile> files = fileRepo.findByTaskId(taskId);
        return files.stream()
                .mapToLong(file -> file.getData() != null ? file.getData().length : 0)
                .sum();
    }
}