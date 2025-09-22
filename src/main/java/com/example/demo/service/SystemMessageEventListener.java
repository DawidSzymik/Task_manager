// src/main/java/com/example/demo/service/SystemMessageEventListener.java - ROZSZERZONY
package com.example.demo.service;

import com.example.demo.controller.TaskController;
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

    // Obsługa wiadomości systemowych w czacie
    @EventListener
    public void handleSystemMessageEvent(SystemMessageEvent event) {
        try {
            messageService.sendSystemMessage(event.getProject(), event.getMessage());
        } catch (Exception e) {
            System.err.println("Błąd przetwarzania eventu systemowego: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Błąd przetwarzania powiadomienia z MessageService: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Błąd przetwarzania powiadomienia z ProjectMemberService: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Błąd przetwarzania powiadomienia z CommentService: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Błąd przetwarzania powiadomienia z FileService: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Błąd przetwarzania powiadomienia z TaskController: " + e.getMessage());
            e.printStackTrace();
        }
    }
}