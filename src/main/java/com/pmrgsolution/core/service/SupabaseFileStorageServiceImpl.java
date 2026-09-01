package com.pmrgsolution.core.service;

import com.pmrgsolution.Exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@Primary
public class SupabaseFileStorageServiceImpl implements FileStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseKey;

    @Value("${supabase.bucket:shreekamalinee}")
    private String bucketName;

    private final ImageOptimizerService imageOptimizerService;
    private final LocalFileStorageServiceImpl localFallbackService;
    private final RestTemplate restTemplate;
    private volatile boolean bucketVerified = false;

    public SupabaseFileStorageServiceImpl(ImageOptimizerService imageOptimizerService, LocalFileStorageServiceImpl localFallbackService) {
        this.imageOptimizerService = imageOptimizerService;
        this.localFallbackService = localFallbackService;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        // Fallback to local storage if Supabase credentials are not provided
        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            log.warn("Supabase credentials not configured. Falling back to local filesystem storage.");
            return localFallbackService.storeFile(file, subDirectory);
        }

        try {
            // 🛡️ SECURITY & OPTIMIZATION: Validate 5 layers, resize to max 1920px, and compress to ~200KB JPEG
            byte[] optimizedBytes = imageOptimizerService.validateAndOptimizeImage(file);

            // Generate clean UUID filename (always .jpg for normalized high-speed web rendering)
            String secureName = UUID.randomUUID().toString() + ".jpg";
            String objectPath = (subDirectory != null && !subDirectory.isBlank()) 
                    ? subDirectory + "/" + secureName 
                    : secureName;

            // Ensure bucket exists in Supabase (auto-create public bucket if missing)
            ensureBucketExists();

            // Construct Supabase Storage upload URL: POST {SUPABASE_URL}/storage/v1/object/{bucket}/{objectPath}
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", 
                    cleanUrl(supabaseUrl), bucketName, objectPath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.IMAGE_JPEG);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(optimizedBytes, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Public CDN URL: {SUPABASE_URL}/storage/v1/object/public/{bucket}/{objectPath}
                String publicUrl = String.format("%s/storage/v1/object/public/%s/%s",
                        cleanUrl(supabaseUrl), bucketName, objectPath);
                log.info("Successfully uploaded optimized image to Supabase: {}", publicUrl);
                return publicUrl;
            } else {
                log.warn("Supabase upload returned non-2xx status: {}. Falling back to local storage.", response.getStatusCode());
                return localFallbackService.storeFile(file, subDirectory);
            }

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.warn("Error communicating with Supabase Storage ({}), falling back to local file storage...", e.getMessage());
            try {
                return localFallbackService.storeFile(file, subDirectory);
            } catch (Exception fallbackEx) {
                log.error("Local file storage fallback also failed: {}", fallbackEx.getMessage());
                throw new BusinessException("Network error uploading file. Please check your internet connection and try again.", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        // If it's a local file, delegate to local service
        if (fileUrl.startsWith("/uploads/")) {
            localFallbackService.deleteFile(fileUrl);
            return;
        }

        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            return;
        }

        try {
            String prefix = String.format("%s/storage/v1/object/public/%s/", cleanUrl(supabaseUrl), bucketName);
            if (fileUrl.startsWith(prefix)) {
                String objectPath = fileUrl.substring(prefix.length());
                String deleteUrl = String.format("%s/storage/v1/object/%s/%s", cleanUrl(supabaseUrl), bucketName, objectPath);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + supabaseKey);
                headers.set("apikey", supabaseKey);

                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, String.class);
                log.info("Successfully deleted image from Supabase: {}", objectPath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from Supabase (non-critical): {}", e.getMessage());
        }
    }

    private void ensureBucketExists() {
        if (bucketVerified) {
            return;
        }
        synchronized (this) {
            if (bucketVerified) {
                return;
            }
            try {
                String checkUrl = String.format("%s/storage/v1/bucket/%s", cleanUrl(supabaseUrl), bucketName);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + supabaseKey);
                headers.set("apikey", supabaseKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Void> checkEntity = new HttpEntity<>(headers);
                try {
                    restTemplate.exchange(checkUrl, HttpMethod.GET, checkEntity, String.class);
                    bucketVerified = true;
                    return; // Bucket exists
                } catch (Exception ex) {
                    // If 404, attempt auto-creation
                    log.info("Supabase bucket '{}' not found. Creating public bucket automatically...", bucketName);
                    String createUrl = String.format("%s/storage/v1/bucket", cleanUrl(supabaseUrl));
                    String body = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"public\":true}", bucketName, bucketName);
                    HttpEntity<String> createEntity = new HttpEntity<>(body, headers);
                    restTemplate.exchange(createUrl, HttpMethod.POST, createEntity, String.class);
                    bucketVerified = true;
                    log.info("Successfully created public Supabase bucket '{}'.", bucketName);
                }
            } catch (Exception e) {
                bucketVerified = true; // Prevent repeated blocking calls if bucket already exists
                log.warn("Could not auto-verify/create Supabase bucket: {}", e.getMessage());
            }
        }
    }

    private String cleanUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
