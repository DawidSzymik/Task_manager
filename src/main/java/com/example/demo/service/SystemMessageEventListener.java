// src/main/java/com/example/demo/service/SystemMessageEventListener.java - NAPRAWIONY
package com.example.demo.service;

import com.example.demo.controller.TaskController;
import com.example.demo.model.Message;
import com.example.demo.service.ProjectMemberService.SystemMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemMessageEventListener {

    @Autowired
    private MessageService messageService;

    @Autowired
    private NotificationService notificationService;

    // NAPRAWIONA - Obsługa wiadomości systemowych w czacie
    @EventListener
    public void handleSystemMessageEvent(SystemMessageEvent event) {
        try {
            // Sprawdź czy projekt jeszcze istnieje
            if (event.getProject() == null || event.getProject().getId() == null) {
                System.err.println("⚠️  Próba wysłania wiadomości systemowej do nieistniejącego projektu");
                return;
            }

            Message savedMessage = messageService.sendSystemMessage(event.getProject(), event.getMessage());

            if (savedMessage != null) {
                System.out.println("✅ Wysłano wiadomość systemową: " + event.getMessage());
            } else {
                System.err.println("⚠️  Nie udało się zapisać wiadomości systemowej (projekt może być w trakcie usuwania)");
            }

        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania eventu systemowego: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU - po prostu zaloguj błąd
        }
    }

    // Obsługa powiadomień z MessageService
    @EventListener
    public void handleMessageNotificationEvent(MessageService.NotificationEvent event) {
        try {
            notificationService.createNotification(
                    event.getUser(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedId(),
                    event.getActionUrl()
            );
        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania powiadomienia z MessageService: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU
        }
    }

    // Obsługa powiadomień z ProjectMemberService
    @EventListener
    public void handleProjectMemberNotificationEvent(ProjectMemberService.NotificationEvent event) {
        try {
            notificationService.createNotification(
                    event.getUser(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedId(),
                    event.getActionUrl()
            );
        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania powiadomienia z ProjectMemberService: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU
        }
    }

    // Obsługa powiadomień z CommentService
    @EventListener
    public void handleCommentNotificationEvent(CommentService.NotificationEvent event) {
        try {
            notificationService.createNotification(
                    event.getUser(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedId(),
                    event.getActionUrl()
            );
        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania powiadomienia z CommentService: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU
        }
    }

    // Obsługa powiadomień z FileService
    @EventListener
    public void handleFileNotificationEvent(FileService.NotificationEvent event) {
        try {
            notificationService.createNotification(
                    event.getUser(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedId(),
                    event.getActionUrl()
            );
        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania powiadomienia z FileService: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU
        }
    }

    // Obsługa powiadomień z TaskController
    @EventListener
    public void handleTaskNotificationEvent(TaskController.NotificationEvent event) {
        try {
            notificationService.createNotification(
                    event.getUser(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedId(),
                    event.getActionUrl()
            );
        } catch (Exception e) {
            System.err.println("❌ Błąd przetwarzania powiadomienia z TaskController: " + e.getMessage());
            // NIE RZUCAJ WYJĄTKU
        }
    }
}