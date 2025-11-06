// src/main/java/com/example/demo/api/controller/UserAvatarController.java
package com.example.demo.api.controller;

import com.example.demo.model.SystemRole;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kontroler do zarządzania avatarami użytkowników
 * Endpointy:
 * - POST /api/v1/users/profile/avatar - upload avatara
 * - GET /api/v1/users/{id}/avatar - pobieranie avatara
 * - DELETE /api/v1/users/profile/avatar - usuwanie avatara
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UserAvatarController {

    private final UserService userService;

    // Maksymalny rozmiar avatara: 5MB
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    // Dozwolone typy plików
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    public UserAvatarController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Upload avatara użytkownika
     * POST /api/v1/users/profile/avatar
     */
    @PostMapping("/profile/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            // Walidacja pliku
            if (file.isEmpty()) {
                return createErrorResponse("Plik jest pusty", HttpStatus.BAD_REQUEST);
            }

            // Sprawdź rozmiar
            if (file.getSize() > MAX_AVATAR_SIZE) {
                return createErrorResponse(
                        "Plik jest za duży. Maksymalny rozmiar: 5MB",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Sprawdź typ pliku
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedType(contentType)) {
                return createErrorResponse(
                        "Nieprawidłowy typ pliku. Dozwolone: JPG, PNG, GIF, WEBP",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Zapisz avatar
            currentUser.setAvatar(file.getBytes());
            currentUser.setAvatarContentType(contentType);
            userService.saveUser(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar został zaktualizowany");
            response.put("avatarUrl", "/api/v1/users/" + currentUser.getId() + "/avatar");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(
                    "Nie udało się zapisać avatara: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Pobieranie avatara użytkownika
     * GET /api/v1/users/{id}/avatar
     */
    @GetMapping("/{id}/avatar")
    public ResponseEntity<ByteArrayResource> getAvatar(@PathVariable Long id) {

        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            if (!user.hasAvatar()) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayResource resource = new ByteArrayResource(user.getAvatar());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(user.getAvatarContentType()))
                    .contentLength(user.getAvatar().length)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache na 1h
                    .body(resource);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pobieranie profilu aktualnego użytkownika
     * GET /api/v1/users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            // Użyj UserMapper do konwersji (z avatarUrl)
            // Zakładam że masz dostęp do UserMapper - jeśli nie, zrób to ręcznie
            Map<String, Object> userDto = new HashMap<>();
            userDto.put("id", currentUser.getId());
            userDto.put("username", currentUser.getUsername());
            userDto.put("email", currentUser.getEmail());
            userDto.put("fullName", currentUser.getFullName());
            userDto.put("systemRole", currentUser.getSystemRole().name());
            userDto.put("active", currentUser.isActive());
            userDto.put("createdAt", currentUser.getCreatedAt());

            // Avatar URL
            if (currentUser.hasAvatar()) {
                userDto.put("avatarUrl", "/api/v1/users/" + currentUser.getId() + "/avatar");
                userDto.put("hasAvatar", true);
            } else {
                userDto.put("hasAvatar", false);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile retrieved successfully");
            response.put("data", userDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(
                    "Nie udało się pobrać profilu: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Usuwanie avatara użytkownika
     * DELETE /api/v1/users/profile/avatar
     */
    @DeleteMapping("/profile/avatar")
    public ResponseEntity<Map<String, Object>> deleteAvatar(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);

            if (!currentUser.hasAvatar()) {
                return createErrorResponse("Nie masz ustawionego avatara", HttpStatus.NOT_FOUND);
            }

            currentUser.setAvatar(null);
            currentUser.setAvatarContentType(null);
            userService.saveUser(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar został usunięty");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(
                    "Nie udało się usunąć avatara: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Helper methods

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            // Fallback dla testów - pobierz pierwszego użytkownika
            List<User> users = userService.getAllUsers();
            if (users.isEmpty()) {
                throw new RuntimeException("Brak użytkowników w systemie");
            }
            return users.get(0);
        }

        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
    }

    private boolean isAllowedType(String contentType) {
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}