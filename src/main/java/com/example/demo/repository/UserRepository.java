// src/main/java/com/example/demo/repository/UserRepository.java
package com.example.demo.repository;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Istniejące metody
    Optional<User> findByUsername(String username);

    // POPRAWIONE NAZWY - pasują do pola 'isActive' w modelu User
    List<User> findByIsActiveTrue();
    List<User> findByIsActiveFalse();
    List<User> findByUsernameContainingIgnoreCase(String username);
    List<User> findBySystemRole(SystemRole systemRole);
    long countByIsActiveTrue();
    boolean existsByUsername(String username);
    long countBySystemRole(SystemRole systemRole);
}