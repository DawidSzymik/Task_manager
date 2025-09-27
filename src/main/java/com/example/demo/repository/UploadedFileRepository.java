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

    List<UploadedFile> findByTask(Task task);
    List<UploadedFile> findByUploadedBy(User uploadedBy);
    List<UploadedFile> findByTaskId(Long taskId);

    // NOWA METODA dla usuwania zadań
    @Modifying
    @Query("DELETE FROM UploadedFile f WHERE f.task = :task")
    void deleteByTask(@Param("task") Task task);
}
