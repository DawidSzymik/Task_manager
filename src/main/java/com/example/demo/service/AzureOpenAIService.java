package com.example.demo.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

@Service
public class AzureOpenAIService {

    private final OpenAIClient client;
    private final String embeddingDeploymentName;
    private final String chatDeploymentName;

    public AzureOpenAIService(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.deployment-embeddings}") String embeddingDeployment,
            @Value("${azure.openai.deployment-chat}") String chatDeployment) {

        this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();

        this.embeddingDeploymentName = embeddingDeployment;
        this.chatDeploymentName = chatDeployment;

        System.out.println("✅ Azure OpenAI Service initialized!");
    }

    /**
     * Tworzy embedding dla tekstu
     */
    public List<Float> createEmbedding(String text) {
        EmbeddingsOptions options = new EmbeddingsOptions(List.of(text));

        Embeddings embeddings = client.getEmbeddings(embeddingDeploymentName, options);

        return embeddings.getData().get(0).getEmbedding();
    }

    /**
     * Generuje odpowiedź chatbota
     */
    public String generateChatResponseWithHistory(
            String userQuery,
            List<String> contextChunks,
            List<Map<String, String>> conversationHistory) {

        try {
            // Zbuduj prompt z kontekstem
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Jesteś inteligentnym asystentem AI dla aplikacji TaskManager. ");
            promptBuilder.append("Pomagasz użytkownikom w organizacji pracy, zarządzaniu zadaniami i projektami.\n\n");

            promptBuilder.append("TWOJE MOŻLIWOŚCI:\n");
            promptBuilder.append("- Odpowiadanie na ogólne pytania\n");
            promptBuilder.append("- Pomaganie w planowaniu i organizacji zadań\n");
            promptBuilder.append("- Udzielanie porad dotyczących produktywności\n");
            promptBuilder.append("- Analizowanie dokumentów użytkownika (gdy są dostępne)\n\n");

            // Dodaj dokumenty TYLKO jeśli są dostępne
            if (contextChunks != null && !contextChunks.isEmpty()) {
                promptBuilder.append("DOSTĘPNE DOKUMENTY UŻYTKOWNIKA:\n");
                for (int i = 0; i < contextChunks.size(); i++) {
                    promptBuilder.append("Dokument ").append(i + 1).append(":\n");
                    promptBuilder.append(contextChunks.get(i)).append("\n\n");
                }
                promptBuilder.append("WAŻNE: Używaj informacji z dokumentów TYLKO gdy są relevantne do pytania użytkownika. ");
                promptBuilder.append("Jeśli pytanie nie dotyczy dokumentów, odpowiedz normalnie jako asystent TaskManagera.\n\n");
            } else {
                promptBuilder.append("(Użytkownik nie ma jeszcze żadnych dokumentów)\n\n");
            }

            promptBuilder.append("Bądź pomocny, przyjazny i konkretny. ");
            promptBuilder.append("Odpowiadaj po polsku.");

            String systemPrompt = promptBuilder.toString();

            // Zbuduj listę wiadomości z historią
            List<ChatRequestMessage> messages = new ArrayList<>();

            // 1. System message
            messages.add(new ChatRequestSystemMessage(systemPrompt));

            // 2. Historia konwersacji (jeśli istnieje)
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                // Ogranicz do ostatnich 10 wiadomości (żeby nie przekroczyć limitu tokenów)
                int startIndex = Math.max(0, conversationHistory.size() - 10);

                for (int i = startIndex; i < conversationHistory.size(); i++) {
                    Map<String, String> msg = conversationHistory.get(i);
                    String role = msg.get("role");
                    String content = msg.get("content");

                    if ("user".equals(role)) {
                        messages.add(new ChatRequestUserMessage(content));
                    } else if ("assistant".equals(role)) {
                        messages.add(new ChatRequestAssistantMessage(content));
                    }
                }
            }

            // 3. Aktualne pytanie
            messages.add(new ChatRequestUserMessage(userQuery));

            System.out.println("🤖 Sending to GPT with " + messages.size() + " messages");

            // Wywołanie Azure OpenAI
            ChatCompletions chatCompletions = client.getChatCompletions(
                    chatDeploymentName,
                    new ChatCompletionsOptions(messages)
                            .setMaxTokens(1000)
                            .setTemperature(0.7)
            );

            String answer = chatCompletions.getChoices().get(0).getMessage().getContent();
            System.out.println("✅ GPT response received: " + answer.length() + " chars");

            return answer;

        } catch (Exception e) {
            System.err.println("❌ GPT error: " + e.getMessage());
            e.printStackTrace();
            return "Przepraszam, wystąpił błąd podczas generowania odpowiedzi.";
        }
    }
}
