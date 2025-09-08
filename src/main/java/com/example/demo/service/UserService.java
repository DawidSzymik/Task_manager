package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Pobranie użytkownika po ID (zwraca Optional)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Pobranie użytkownika po nazwie użytkownika (zwraca Optional)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Pobranie wszystkich użytkowników
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Rejestracja nowego użytkownika
    public User registerNewUser(String username, String password) {
        // Sprawdzamy, czy użytkownik już istnieje
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Użytkownik już istnieje!");
        }

        // Tworzymy nowego użytkownika i zapisujemy w bazie
        User user = new User(username, passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}
