// src/main/java/com/example/demo/api/controller/UserAvatarController.java
package com.example.demo.api.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UserAvatarController.class);

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

        logger.info("📤 Avatar upload request from user: {}", userDetails.getUsername());

        try {
            User currentUser = getCurrentUser(userDetails);
            logger.info("✅ Current user found: {} (ID: {})", currentUser.getUsername(), currentUser.getId());

            // Walidacja pliku
            if (file.isEmpty()) {
                logger.warn("❌ Empty file uploaded");
                return createErrorResponse("Plik jest pusty", HttpStatus.BAD_REQUEST);
            }

            logger.info("📁 File info: name={}, size={}, type={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Sprawdź rozmiar
            if (file.getSize() > MAX_AVATAR_SIZE) {
                logger.warn("❌ File too large: {} bytes", file.getSize());
                return createErrorResponse(
                        "Plik jest za duży. Maksymalny rozmiar: 5MB",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Sprawdź typ pliku
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedType(contentType)) {
                logger.warn("❌ Invalid file type: {}", contentType);
                return createErrorResponse(
                        "Nieprawidłowy typ pliku. Dozwolone: JPG, PNG, GIF, WEBP",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Zapisz avatar
            logger.info("💾 Saving avatar to database...");
            byte[] avatarBytes = file.getBytes();
            currentUser.setAvatar(avatarBytes);
            currentUser.setAvatarContentType(contentType);

            User savedUser = userService.saveUser(currentUser);
            logger.info("✅ Avatar saved successfully for user: {}", savedUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar został zaktualizowany");
            response.put("avatarUrl", "/api/v1/users/" + currentUser.getId() + "/avatar");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error uploading avatar", e);
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
            logger.debug("📥 Fetching avatar for user ID: {}", id);

            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            if (!user.hasAvatar()) {
                logger.debug("⚠️ User {} has no avatar", id);
                return ResponseEntity.notFound().build();
            }

            ByteArrayResource resource = new ByteArrayResource(user.getAvatar());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(user.getAvatarContentType()))
                    .contentLength(user.getAvatar().length)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache na 1h
                    .body(resource);

        } catch (RuntimeException e) {
            logger.warn("❌ User not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error fetching avatar for user: {}", id, e);
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
            logger.error("❌ Error fetching profile", e);
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
            logger.info("🗑️ Deleting avatar for user: {}", userDetails.getUsername());

            User currentUser = getCurrentUser(userDetails);

            if (!currentUser.hasAvatar()) {
                logger.warn("⚠️ User {} has no avatar to delete", currentUser.getUsername());
                return createErrorResponse("Nie masz ustawionego avatara", HttpStatus.NOT_FOUND);
            }

            currentUser.setAvatar(null);
            currentUser.setAvatarContentType(null);
            userService.saveUser(currentUser);

            logger.info("✅ Avatar deleted successfully for user: {}", currentUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar został usunięty");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error deleting avatar", e);
            return createErrorResponse(
                    "Nie udało się usunąć avatara: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Pobiera aktualnie zalogowanego użytkownika
     */
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
    }

    /**
     * Sprawdza czy typ pliku jest dozwolony
     */
    private boolean isAllowedType(String contentType) {
        for (String allowedType : ALLOWED_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tworzy odpowiedź błędu
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}