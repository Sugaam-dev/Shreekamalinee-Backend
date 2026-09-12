package com.pmrgsolution.core.service;

import com.pmrgsolution.exception.BusinessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path rootDir = Paths.get("uploads");
    private final ImageOptimizerService imageOptimizerService;

    public LocalFileStorageServiceImpl(@Lazy ImageOptimizerService imageOptimizerService) {
        this.imageOptimizerService = imageOptimizerService;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("Cannot store empty file", HttpStatus.BAD_REQUEST);
            }

            // Create sub-folder (e.g. uploads/products, uploads/payment-proofs)
            Path uploadPath = rootDir.resolve(subDirectory);
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            boolean isPdf = (contentType != null && contentType.equalsIgnoreCase("application/pdf"))
                    || (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));

            String secureName;
            byte[] fileBytes;

            if (isPdf) {
                // PDF Document Validation (e.g. payment proofs / receipts)
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                    throw new BusinessException("PDF receipt file size exceeds 10MB limit", HttpStatus.BAD_REQUEST);
                }
                fileBytes = file.getBytes();
                // Magic byte verification for PDF (%PDF-)
                if (fileBytes.length < 5 || fileBytes[0] != 0x25 || fileBytes[1] != 0x50 || fileBytes[2] != 0x44 || fileBytes[3] != 0x46) {
                    throw new BusinessException("Invalid PDF file structure", HttpStatus.BAD_REQUEST);
                }
                secureName = UUID.randomUUID().toString() + ".pdf";
            } else {
                // 🛡️ SECURITY & OPTIMIZATION: Validate 5 layers, resize to max 1920px, and compress to ~200KB JPEG
                fileBytes = imageOptimizerService.validateAndOptimizeImage(file);
                secureName = UUID.randomUUID().toString() + ".jpg";
            }

            Path targetLocation = uploadPath.resolve(secureName);
            // Save to disk
            Files.write(targetLocation, fileBytes);

            // Return relative URL for static serving
            return "/uploads/" + subDirectory + "/" + secureName;

        } catch (BusinessException be) {
            throw be;
        } catch (IOException e) {
            throw new BusinessException("Failed to store file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            String relativePath = fileUrl.substring("/uploads/".length());
            Path filePath = rootDir.resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning but don't crash
        }
    }
}
