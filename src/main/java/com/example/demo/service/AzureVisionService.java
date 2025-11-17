package com.example.demo.service;

import com.azure.ai.vision.imageanalysis.*;
import com.azure.ai.vision.imageanalysis.models.*;
import com.azure.core.credential.KeyCredential;
import com.azure.core.util.BinaryData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AzureVisionService {

    private final ImageAnalysisClient client;

    public AzureVisionService(
            @Value("${azure.vision.endpoint}") String endpoint,
            @Value("${azure.vision.api-key}") String apiKey) {

        this.client = new ImageAnalysisClientBuilder()
                .endpoint(endpoint)
                .credential(new KeyCredential(apiKey))
                .buildClient();

        System.out.println("✅ Azure Vision Service initialized!");
    }

    /**
     * Wyciąga tekst z obrazu (OCR)
     */
    public String extractTextFromImage(byte[] imageData) {
        try {
            ImageAnalysisResult result = client.analyze(
                    BinaryData.fromBytes(imageData),
                    Arrays.asList(VisualFeatures.READ),
                    new ImageAnalysisOptions()
            );

            if (result.getRead() != null && result.getRead().getBlocks() != null) {
                StringBuilder text = new StringBuilder();

                for (DetectedTextBlock block : result.getRead().getBlocks()) {
                    for (DetectedTextLine line : block.getLines()) {
                        text.append(line.getText()).append("\n");
                    }
                }

                return text.toString();
            }

            return "";

        } catch (Exception e) {
            System.err.println("❌ OCR error: " + e.getMessage());
            return "";
        }
    }
}