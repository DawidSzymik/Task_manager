// src/main/java/com/example/demo/model/NotificationType.java - ROZSZERZONE
package com.example.demo.model;

public enum NotificationType {
    TASK_ASSIGNED,              // Przypisanie do zadania
    TASK_STATUS_CHANGED,        // Zmiana statusu zadania
    TASK_PROPOSAL_PENDING,      // Nowa propozycja zadania
    TASK_PROPOSAL_APPROVED,     // Propozycja zatwierdzona
    TASK_PROPOSAL_REJECTED,     // Propozycja odrzucona
    STATUS_CHANGE_PENDING,      // Prośba o zmianę statusu
    STATUS_CHANGE_APPROVED,     // Zmiana statusu zatwierdzona
    STATUS_CHANGE_REJECTED,     // Zmiana statusu odrzucona
    NEW_ACTIVITY,               // Nowa aktywność (komentarz/plik)
    ROLE_CHANGED,               // Zmiana roli w projekcie

    // NOWE POWIADOMIENIA
    NEW_MESSAGE,                // Nowa wiadomość w czacie projektu
    PROJECT_MEMBER_ADDED,       // Dodano Cię do projektu
    TASK_COMMENT_ADDED,         // Nowy komentarz w zadaniu
    TASK_FILE_UPLOADED          // Nowy plik w zadaniu
}