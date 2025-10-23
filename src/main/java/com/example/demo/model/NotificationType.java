// src/main/java/com/example/demo/model/NotificationType.java
package com.example.demo.model;

public enum NotificationType {
    // Task-related notifications
    TASK_ASSIGNED,              // Przypisanie do zadania
    TASK_STATUS_CHANGED,        // Zmiana statusu zadania
    TASK_COMMENT_ADDED,         // Nowy komentarz w zadaniu
    TASK_FILE_UPLOADED,         // Nowy plik w zadaniu

    // Task proposals
    TASK_PROPOSAL_PENDING,      // Nowa propozycja zadania
    TASK_PROPOSAL_APPROVED,     // Propozycja zatwierdzona
    TASK_PROPOSAL_REJECTED,     // Propozycja odrzucona

    // Status change requests
    STATUS_CHANGE_PENDING,      // Prośba o zmianę statusu
    STATUS_CHANGE_APPROVED,     // Zmiana statusu zatwierdzona
    STATUS_CHANGE_REJECTED,     // Zmiana statusu odrzucona

    // Project-related notifications
    PROJECT_MEMBER_ADDED,       // Dodano Cię do projektu
    PROJECT_MEMBER_REMOVED,     // Usunięto z projektu
    PROJECT_ROLE_CHANGED,       // Zmiana roli w projekcie

    // Messages
    NEW_MESSAGE,                // Nowa wiadomość w czacie projektu

    // Generic
    NEW_ACTIVITY,               // Nowa aktywność (ogólne)
    ROLE_CHANGED                // Zmiana roli (deprecated - use PROJECT_ROLE_CHANGED)
}