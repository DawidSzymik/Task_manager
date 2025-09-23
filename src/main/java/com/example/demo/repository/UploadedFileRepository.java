package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.UploadedFile;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByTask(Task task);
    List<UploadedFile> findByUploadedBy(User uploadedBy);
    void deleteByTask(Task task);

    // DODAJ TE METODY
    List<UploadedFile> findByTaskId(Long taskId);
}