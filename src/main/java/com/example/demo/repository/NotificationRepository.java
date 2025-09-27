// src/main/java/com/example/demo/repository/NotificationRepository.java
package com.example.demo.repository;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean isRead);
    List<Notification> findByUser(User user);
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    // ZMIENIONA METODA - zwraca long zamiast int
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadByUser(@Param("user") User user);

    // Metoda dla usuwania projektów
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.relatedId = :relatedId")
    void deleteByRelatedEntityId(@Param("relatedId") Long relatedId);
}