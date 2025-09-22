// src/main/java/com/example/demo/service/MessageService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    // Wysłanie zwykłej wiadomości
    @Transactional
    public Message sendMessage(Project project, User author, String content) {
        // Sprawdź czy user jest członkiem projektu
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectAndUser(project, author);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Użytkownik nie jest członkiem projektu");
        }

        // Sprawdź uprawnienia
        ProjectRole userRole = memberOpt.get().getRole();
        if (userRole == ProjectRole.VIEWER) {
            throw new RuntimeException("Viewerzy nie mogą wysyłać wiadomości");
        }

        Message message = new Message(content, project, author);
        return messageRepository.save(message);
    }

    // Wysłanie wiadomości systemowej - NAPRAWIONE
    @Transactional
    public void sendSystemMessage(Project project, String content) {
        try {
            Message systemMessage = new Message(content, project); // Bez autora
            messageRepository.save(systemMessage);
            System.out.println("✅ Zapisano wiadomość systemową: " + content);
        } catch (Exception e) {
            System.err.println("❌ Błąd zapisywania wiadomości systemowej: " + e.getMessage());
            // Nie rzucamy wyjątku - tylko logujemy
        }
    }

    // Pobranie wiadomości projektu
    public List<Message> getProjectMessages(Project project) {
        return messageRepository.findByProjectOrderByCreatedAtAsc(project);
    }

    // Pobranie ostatnich wiadomości
    public List<Message> getRecentMessages(Project project, int limit) {
        List<Message> messages = messageRepository.findTop50ByProjectOrderByCreatedAtDesc(project);
        if (messages.size() > limit) {
            messages = messages.subList(0, limit);
        }
        Collections.reverse(messages);
        return messages;
    }

    // Edycja wiadomości
    @Transactional
    public Message editMessage(Long messageId, User editor, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Wiadomość nie istnieje"));

        if (!message.canBeEditedBy(editor)) {
            throw new RuntimeException("Nie możesz edytować tej wiadomości");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    // Usuwanie wiadomości
    @Transactional
    public void deleteMessage(Long messageId, User deleter) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Wiadomość nie istnieje"));

        if (!message.canBeDeletedBy(deleter)) {
            throw new RuntimeException("Nie możesz usunąć tej wiadomości");
        }

        messageRepository.delete(message);
    }

    // Wyszukiwanie wiadomości
    public List<Message> searchMessages(Project project, String searchTerm) {
        return messageRepository.findByProjectAndContentContainingIgnoreCase(project, searchTerm);
    }

    // Usunięcie wszystkich wiadomości projektu
    @Transactional
    public void deleteAllProjectMessages(Project project) {
        try {
            List<Message> messages = messageRepository.findByProjectOrderByCreatedAtAsc(project);
            if (!messages.isEmpty()) {
                messageRepository.deleteAll(messages);
                System.out.println("✅ Usunięto " + messages.size() + " wiadomości z projektu: " + project.getName());
            }
        } catch (Exception e) {
            System.err.println("❌ Błąd usuwania wiadomości projektu: " + e.getMessage());
        }
    }
}