// src/main/java/com/example/demo/config/DataInitializer.java
package com.example.demo.config;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createSuperAdminIfNotExists();
    }

    private void createSuperAdminIfNotExists() {
        String adminUsername = "admin";

        // Sprawdź czy admin już istnieje
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            System.out.println("✅ Konto Super Administratora już istnieje: " + adminUsername);
            return;
        }

        // Utwórz super admina
        User superAdmin = new User();
        superAdmin.setUsername(adminUsername);
        superAdmin.setPassword(passwordEncoder.encode("123456"));
        superAdmin.setEmail("admin@taskmanager.local");
        superAdmin.setFullName("Super Administrator");
        superAdmin.setSystemRole(SystemRole.SUPER_ADMIN);
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setActive(true);

        userRepository.save(superAdmin);

        System.out.println("🔥 UTWORZONO KONTO SUPER ADMINISTRATORA! 🔥");
        System.out.println("Login: admin");
        System.out.println("Hasło: 123456");
        System.out.println("⚠️  ZMIEŃ HASŁO PO PIERWSZYM LOGOWANIU! ⚠️");
        System.out.println("===============================================");
    }
}
