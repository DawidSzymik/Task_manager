package com.example.demo.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    public String generateChatResponse(String userQuestion, List<String> contextChunks) {

        // Zbuduj prompt
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Na podstawie poniższych informacji z dokumentów projektu:\n\n");

        for (int i = 0; i < contextChunks.size(); i++) {
            contextBuilder.append("Dokument ").append(i + 1).append(":\n");
            contextBuilder.append(contextChunks.get(i)).append("\n\n");
        }

        contextBuilder.append("Odpowiedz na pytanie użytkownika: ").append(userQuestion);

        // Wywołaj GPT
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage("Jesteś pomocnym asystentem AI który odpowiada na pytania na podstawie dokumentów projektowych."));
        messages.add(new ChatRequestUserMessage(contextBuilder.toString()));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(500);
        options.setTemperature(0.7);

        ChatCompletions completions = client.getChatCompletions(chatDeploymentName, options);

        return completions.getChoices().get(0).getMessage().getContent();
    }
}