// src/main/java/com/example/demo/repository/UploadedFileRepository.java
package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.UploadedFile;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    // Znajdź pliki dla zadania
    List<UploadedFile> findByTask(Task task);

    // Znajdź pliki dla zadania posortowane po dacie (najnowsze najpierw)
    List<UploadedFile> findByTaskOrderByUploadedAtDesc(Task task);

    // Znajdź pliki dla zadania posortowane po dacie (najstarsze najpierw)
    List<UploadedFile> findByTaskOrderByUploadedAtAsc(Task task);

    // Policz pliki dla zadania
    long countByTask(Task task);

    // Usuń wszystkie pliki dla zadania
    @Modifying
    @Query("DELETE FROM UploadedFile f WHERE f.task = :task")
    void deleteByTask(@Param("task") Task task);

    // Znajdź pliki użytkownika
    List<UploadedFile> findByUploadedBy(User user);

    // Znajdź pliki użytkownika dla zadania
    List<UploadedFile> findByTaskAndUploadedBy(Task task, User user);

    // Policz pliki użytkownika
    long countByUploadedBy(User user);
}