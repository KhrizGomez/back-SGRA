package com.CLMTZ.Backend.service.external.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.service.external.IStorageService;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class StorageService implements IStorageService{

    private final BlobServiceClient blobServiceClient;

    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    public String uploadFiles(MultipartFile file){

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                containerClient.create();
            }

            String originalFilename = file.getOriginalFilename();
            String fileName = originalFilename;

            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();
        } catch (Exception e) {
            throw new RuntimeException("Error al subir el archivo: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadFile(String fileName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Archivo no encontrado: " + fileName);
        }
    }

    @Override
    public String getContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        return "application/octet-stream";
    }
}
