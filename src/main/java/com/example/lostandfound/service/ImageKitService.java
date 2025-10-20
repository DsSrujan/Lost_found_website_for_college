package com.example.lostandfound.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;

@Service
public class ImageKitService {

    private final ImageKit imageKit;

    public ImageKitService() {
        // Read credentials from Render's environment variables
        String publicKey = System.getenv("IMAGEKIT_PUBLIC_KEY");
        String privateKey = System.getenv("IMAGEKIT_PRIVATE_KEY");
        String urlEndpoint = System.getenv("IMAGEKIT_URL_ENDPOINT");

        Configuration config = new Configuration(publicKey, privateKey, urlEndpoint);
        this.imageKit = ImageKit.getInstance();
        this.imageKit.setConfig(config);
    }

    /**
     * Uploads a file and returns both the public URL and the unique file ID.
     * @param file The multipart file to upload.
     * @return A Map containing the "url" and "fileId".
     */
    public Map<String, String> uploadFile(MultipartFile file) {
        try {
            // Generate a unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // Create the upload request
            FileCreateRequest fileCreateRequest = new FileCreateRequest(file.getBytes(), fileName);
            
            // Execute the upload
            Result result = imageKit.upload(fileCreateRequest);
            
            // Prepare the map to return both URL and File ID
            Map<String, String> uploadData = new HashMap<>();
            uploadData.put("url", result.getUrl());
            uploadData.put("fileId", result.getFileId());
            
            return uploadData;
        } catch (Exception e) {
            throw new RuntimeException("Could not upload file to ImageKit", e);
        }
    }

    /**
     * Deletes a file from ImageKit using its unique file ID.
     * @param fileId The unique ID of the file to delete.
     */
    public void deleteFile(String fileId) {
        try {
            imageKit.deleteFile(fileId);
        } catch (Exception e) {
            // Log the error but don't crash the application
            System.err.println("Could not delete file from ImageKit: " + e.getMessage());
        }
    }
}

