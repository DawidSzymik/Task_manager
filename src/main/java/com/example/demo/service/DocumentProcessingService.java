package com.example.demo.service;

import com.example.demo.model.DocumentEmbedding;
import com.example.demo.model.UploadedFile;
import com.example.demo.repository.DocumentEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentProcessingService {
    // ... reszta kodu

    @Autowired
    private AzureBlobService blobService;

    @Autowired
    private AzureOpenAIService openAIService;

    @Autowired
    private DocumentEmbeddingRepository embeddingRepository;

    @Autowired
    private AzureVisionService visionService;  // ✅ DODAJ TO

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Przetwarza plik - tworzy chunki i embeddingi
     */
    @Transactional
    public void processFile(UploadedFile file) {
        try {
            System.out.println("🔄 Processing file: " + file.getOriginalName());

            // 1. Pobierz plik z Blob Storage
            byte[] fileData = blobService.downloadFile(file.getBlobUrl());

            // 2. Wyciągnij tekst z pliku
            String text = extractTextFromFile(fileData, file.getContentType());

            if (text == null || text.trim().isEmpty()) {
                System.out.println("⚠️ No text extracted from file");
                return;
            }

            // 3. Podziel na chunki (kawałki ~500 słów)
            List<String> chunks = splitIntoChunks(text, 500);
            System.out.println("📄 Created " + chunks.size() + " chunks");

            // 4. Dla każdego chunka stwórz embedding
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                // Stwórz embedding
                List<Float> embeddingVector = openAIService.createEmbedding(chunk);

                // Konwertuj wektor na JSON string
                String embeddingJson = objectMapper.writeValueAsString(embeddingVector);

                // Zapisz do bazy
                DocumentEmbedding embedding = new DocumentEmbedding();
                embedding.setFileId(file.getId());
                embedding.setChunkIndex(i);
                embedding.setChunkText(chunk);
                embedding.setEmbedding(embeddingJson);

                embeddingRepository.save(embedding);

                System.out.println("✅ Chunk " + (i + 1) + "/" + chunks.size() + " processed");
            }

            System.out.println("✅ File processed successfully: " + file.getOriginalName());

        } catch (Exception e) {
            System.err.println("❌ Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Wyciąga tekst z pliku w zależności od typu
     */
    private String extractTextFromFile(byte[] fileData, String contentType) throws IOException {
        if (contentType.contains("pdf")) {
            return extractTextFromPDF(fileData);
        } else if (contentType.contains("excel") || contentType.contains("spreadsheet")) {
            return extractTextFromExcel(fileData);
        } else if (contentType.contains("word") || contentType.contains("document")) {
            return extractTextFromWord(fileData);
        } else {
            System.out.println("⚠️ Unsupported file type: " + contentType);
            return null;
        }
    }

    /**
     * PDF → tekst (z OCR dla obrazów)
     */
    private String extractTextFromPDF(byte[] fileData) throws IOException {
        StringBuilder allText = new StringBuilder();

        try (PDDocument document = PDDocument.load(fileData)) {
            // 1. Wyciągnij normalny tekst
            PDFTextStripper stripper = new PDFTextStripper();
            String regularText = stripper.getText(document);
            allText.append(regularText);

            System.out.println("📄 Extracted text from PDF: " + regularText.length() + " chars");

            // 2. Wyciągnij tekst z obrazów (OCR)
            try {
                int imageCount = 0;

                for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                    PDPage page = document.getPage(pageNum);
                    PDResources resources = page.getResources();

                    if (resources != null) {
                        for (COSName name : resources.getXObjectNames()) {
                            try {
                                PDImageXObject image = (PDImageXObject) resources.getXObject(name);

                                if (image != null) {
                                    // Konwertuj do byte[]
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ImageIO.write(image.getImage(), "png", baos);
                                    byte[] imageBytes = baos.toByteArray();

                                    // OCR na obrazie
                                    String imageText = visionService.extractTextFromImage(imageBytes);

                                    if (!imageText.trim().isEmpty()) {
                                        allText.append("\n\n[Tekst z obrazu ").append(++imageCount).append("]:\n");
                                        allText.append(imageText);
                                        System.out.println("🖼️ Extracted text from image " + imageCount + ": " + imageText.length() + " chars");
                                    }
                                }
                            } catch (ClassCastException e) {
                                // Not an image, skip
                            }
                        }
                    }
                }

                if (imageCount > 0) {
                    System.out.println("✅ Total images processed: " + imageCount);
                }

            } catch (Exception e) {
                System.err.println("⚠️ OCR processing failed (continuing with text only): " + e.getMessage());
            }
        }

        return allText.toString();
    }

    /**
     * Excel → tekst
     */
    private String extractTextFromExcel(byte[] fileData) throws IOException {
        StringBuilder text = new StringBuilder();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileData))) {
            for (Sheet sheet : workbook) {
                text.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                for (Row row : sheet) {
                    for (Cell cell : row) {
                        text.append(getCellValueAsString(cell)).append("\t");
                    }
                    text.append("\n");
                }
                text.append("\n");
            }
        }

        return text.toString();
    }

    /**
     * Word → tekst
     */
    private String extractTextFromWord(byte[] fileData) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileData))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    /**
     * Helper - konwertuje komórkę Excel na string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Dzieli tekst na chunki (kawałki ~maxWords słów każdy)
     */
    private List<String> splitIntoChunks(String text, int maxWords) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i += maxWords) {
            int end = Math.min(i + maxWords, words.length);
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(words, i, end));
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Usuń embeddingi dla danego pliku (gdy plik jest usuwany)
     */
    @Transactional
    public void deleteEmbeddingsForFile(Long fileId) {
        embeddingRepository.deleteByFileId(fileId);
        System.out.println("🗑️ Deleted embeddings for file: " + fileId);
    }
}