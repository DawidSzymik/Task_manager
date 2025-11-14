package com.example.demo.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AzureBlobService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public AzureBlobService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName) {

        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        this.containerName = containerName;

        System.out.println("✅ AzureBlobService initialized!");
        System.out.println("📦 Container: " + containerName);
    }

    /**
     * Upload pliku do Azure Blob Storage
     * @return URL do pliku w Azure
     */
    public String uploadFile(MultipartFile file, Long projectId) throws IOException {

        System.out.println("📤 Uploading file: " + file.getOriginalFilename());
        System.out.println("📦 Size: " + file.getSize() + " bytes");

        // Generuj unikalną nazwę pliku
        String fileName = projectId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Pobierz container
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // Pobierz blob client
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // Upload
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        String blobUrl = blobClient.getBlobUrl();
        System.out.println("✅ File uploaded: " + blobUrl);

        return blobUrl;
    }

    /**
     * Download pliku z Azure Blob Storage
     * @return byte array pliku
     */
    public byte[] downloadFile(String blobUrl) {

        System.out.println("📥 Downloading file from: " + blobUrl);

        try {
            // Wyciągnij nazwę blob z URL
            String blobName = extractBlobNameFromUrl(blobUrl);

            System.out.println("📦 Blob name: " + blobName);

            // ✅ Użyj istniejącego blobServiceClient (ma connection string!)
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            // Download do ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);

            byte[] data = outputStream.toByteArray();
            System.out.println("✅ File downloaded: " + data.length + " bytes");

            return data;

        } catch (Exception e) {
            System.err.println("❌ Error downloading file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to download file from Azure: " + e.getMessage());
        }
    }

    /**
     * Usuń plik z Azure Blob Storage
     */
    public void deleteFile(String blobUrl) {

        System.out.println("🗑️ Deleting file: " + blobUrl);

        try {
            String blobName = extractBlobNameFromUrl(blobUrl);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            blobClient.delete();

            System.out.println("✅ File deleted from Azure");

        } catch (Exception e) {
            System.err.println("❌ Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Generuj tymczasowy URL z dostępem (SAS token)
     * @param blobUrl URL do pliku
     * @param minutesValid Ile minut URL ma być ważny
     * @return URL z SAS tokenem
     */
    public String getDownloadUrlWithSAS(String blobUrl, int minutesValid) {

        try {
            String blobName = extractBlobNameFromUrl(blobUrl);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            // Ustawienia SAS
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(minutesValid);

            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permission);

            String sasToken = blobClient.generateSas(sasValues);

            return blobUrl + "?" + sasToken;

        } catch (Exception e) {
            System.err.println("❌ Error generating SAS: " + e.getMessage());
            return blobUrl; // Fallback do zwykłego URL
        }
    }

    /**
     * Wyciąga nazwę blob z pełnego URL
     * https://storage.blob.core.windows.net/container/project/file.pdf -> project/file.pdf
     */
    private String extractBlobNameFromUrl(String blobUrl) {
        try {
            // URL format: https://accountname.blob.core.windows.net/container/blobname
            String[] parts = blobUrl.split("/" + containerName + "/");
            if (parts.length > 1) {
                return parts[1];
            }
            throw new RuntimeException("Invalid blob URL format");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract blob name from URL: " + blobUrl);
        }
    }
}