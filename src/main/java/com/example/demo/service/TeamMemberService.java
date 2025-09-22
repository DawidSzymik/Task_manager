// src/main/java/com/example/demo/service/TeamMemberService.java
package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamMemberService {

    // Usuń użytkownika ze wszystkich zespołów
    @Transactional
    public void removeUserFromAllTeams(User user) {
        try {
            // Jeśli masz tabele zespołów, usuń relacje tutaj
            // Na razie tylko loguj
            System.out.println("Usuwanie użytkownika " + user.getUsername() + " ze wszystkich zespołów");

            // Przykład - jeśli masz relację Many-to-Many:
            // user.getTeams().clear();
            // userRepository.save(user);

        } catch (Exception e) {
            System.err.println("Błąd usuwania użytkownika z zespołów: " + e.getMessage());
        }
    }
}