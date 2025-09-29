// src/main/java/com/example/demo/repository/TeamRepository.java
package com.example.demo.repository;

import com.example.demo.model.Team;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    // POPRAWIONE: zmieniono z findByUsersContaining na findByMembersContaining
    // bo w modelu Team pole nazywa się "members"
    List<Team> findByMembersContaining(User user);

    // Znajdź zespoły utworzone przez użytkownika
    List<Team> findByCreatedBy(User user);

    // Znajdź zespół po nazwie
    Optional<Team> findByName(String name);

    // Znajdź zespoły po nazwie (like)
    List<Team> findByNameContainingIgnoreCase(String name);

    // Sprawdź czy istnieje zespół o danej nazwie
    boolean existsByName(String name);
}