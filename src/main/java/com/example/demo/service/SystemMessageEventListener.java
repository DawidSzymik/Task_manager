// src/main/java/com/example/demo/service/SystemMessageEventListener.java
package com.example.demo.service;

import com.example.demo.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemMessageEventListener {

    @Autowired
    private MessageService messageService;

    @EventListener
    public void handleSystemMessageEvent(SystemMessageEvent event) {
        try {
            messageService.sendSystemMessage(event.getProject(), event.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Błąd obsługi eventu systemowego: " + e.getMessage());
        }
    }

    // Klasa eventu
    public static class SystemMessageEvent {
        private final Project project;
        private final String message;

        public SystemMessageEvent(Project project, String message) {
            this.project = project;
            this.message = message;
        }

        public Project getProject() { return project; }
        public String getMessage() { return message; }
    }
}