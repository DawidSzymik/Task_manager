// src/main/java/com/example/demo/repository/UserRepository.java - ROZSZERZONY
package com.example.demo.repository;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // NOWE - Zapytania dla systemu administracyjnego
    List<User> findBySystemRole(SystemRole systemRole);
    List<User> findByIsActiveTrue();
    List<User> findByIsActiveFalse();
    Optional<User> findByEmail(String email);
    List<User> findByUsernameContainingIgnoreCase(String username);
    List<User> findByFullNameContainingIgnoreCase(String fullName);
}