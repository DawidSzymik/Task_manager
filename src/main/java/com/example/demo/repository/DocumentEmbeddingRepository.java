package com.example.demo.repository;

import com.example.demo.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Integer> {

    // Znajdź wszystkie embeddingi dla danego pliku
    List<DocumentEmbedding> findByFileId(Long fileId);

    // Usuń wszystkie embeddingi dla danego pliku
    void deleteByFileId(Long fileId);

    // Znajdź embeddingi tylko z plików do których user ma dostęp (przez zadania)
    @Query(value = """
        SELECT e.* FROM document_embeddings e
        INNER JOIN uploaded_files f ON e.file_id = f.id
        INNER JOIN task t ON f.task_id = t.id
        INNER JOIN task_users tu ON t.id = tu.task_id
        WHERE tu.user_id = :userId
        """, nativeQuery = true)
    List<DocumentEmbedding> findAllAccessibleByUser(@Param("userId") Long userId);
}