package com.example.demo.api.controller;

import com.example.demo.model.DocumentEmbedding;
import com.example.demo.model.User;
import com.example.demo.repository.DocumentEmbeddingRepository;
import com.example.demo.service.AzureOpenAIService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chatbot")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class ChatbotApiController {

    @Autowired
    private AzureOpenAIService openAIService;

    @Autowired
    private DocumentEmbeddingRepository embeddingRepository;

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Endpoint chatbota - odpowiada na pytania na podstawie dokumentów
     * POST /api/v1/chatbot/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        try {
            String query = (String) request.get("query");

            // ✅ DODANE - pobierz historię konwersacji
            List<Map<String, String>> conversationHistory =
                    (List<Map<String, String>>) request.get("conversationHistory");

            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Query is required"
                ));
            }

            // Pobierz username z session
            HttpSession session = httpRequest.getSession(false);
            String username = null;

            if (session != null) {
                Object securityContextObj = session.getAttribute("SPRING_SECURITY_CONTEXT");
                if (securityContextObj != null) {
                    org.springframework.security.core.context.SecurityContext securityContext =
                            (org.springframework.security.core.context.SecurityContext) securityContextObj;

                    if (securityContext.getAuthentication() != null) {
                        username = securityContext.getAuthentication().getName();
                    }
                }
            }

            if (username == null || username.equals("anonymousUser")) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "answer", "Musisz być zalogowany, aby korzystać z asystenta AI.",
                        "sources", List.of()
                ));
            }

            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("💬 Chatbot query from user: " + currentUser.getUsername());
            System.out.println("📝 Query: " + query);
            System.out.println("📚 Conversation history size: " +
                    (conversationHistory != null ? conversationHistory.size() : 0));

            // 1. Stwórz embedding dla pytania użytkownika
            List<Float> queryEmbedding = openAIService.createEmbedding(query);

            // 2. Pobierz WSZYSTKIE embeddingi do których user ma dostęp
            List<DocumentEmbedding> accessibleEmbeddings =
                    embeddingRepository.findAllAccessibleByUser(currentUser.getId());

            if (accessibleEmbeddings.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "answer", "Nie masz jeszcze żadnych dokumentów w swoich zadaniach. Dodaj pliki do zadań, aby móc zadawać pytania.",
                        "sources", List.of()
                ));
            }

            System.out.println("📚 Found " + accessibleEmbeddings.size() + " accessible chunks");

            // 3. Znajdź najbardziej podobne chunki
            List<ScoredChunk> scoredChunks = new ArrayList<>();

            for (DocumentEmbedding embedding : accessibleEmbeddings) {
                List<Float> chunkEmbedding = objectMapper.readValue(
                        embedding.getEmbedding(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Float.class)
                );

                double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                scoredChunks.add(new ScoredChunk(embedding, similarity));
            }

            scoredChunks.sort((a, b) -> Double.compare(b.score, a.score));

            List<String> topChunks = scoredChunks.stream()
                    .limit(3)
                    .map(sc -> sc.embedding.getChunkText())
                    .collect(Collectors.toList());

            System.out.println("🎯 Top 3 chunks similarity scores: " +
                    scoredChunks.stream().limit(3).map(sc -> sc.score).collect(Collectors.toList()));

            // 4. Wygeneruj odpowiedź z historią
            String answer = openAIService.generateChatResponseWithHistory(
                    query,
                    topChunks,
                    conversationHistory  // ✅ DODANE - przekaż historię!
            );

            // 5. Przygotuj listę źródeł
            Set<Long> sourceFileIds = scoredChunks.stream()
                    .limit(3)
                    .map(sc -> sc.embedding.getFileId())
                    .collect(Collectors.toSet());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", answer);
            response.put("sources", sourceFileIds);
            response.put("chunksUsed", topChunks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to process query: " + e.getMessage()
            ));
        }
    }

    /**
     * Oblicza cosine similarity między dwoma wektorami
     */
    private double cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Helper class - chunk z jego similarity score
     */
    private static class ScoredChunk {
        DocumentEmbedding embedding;
        double score;

        ScoredChunk(DocumentEmbedding embedding, double score) {
            this.embedding = embedding;
            this.score = score;
        }
    }
}