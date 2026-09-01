package com.pmrgsolution.core.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageOptimizerService {
    
    /**
     * Validates, resizes, strips metadata, and compresses an uploaded image.
     * 
     * @param file The raw uploaded MultipartFile
     * @return Optimized byte array of the image (ready for storage)
     */
    byte[] validateAndOptimizeImage(MultipartFile file);
}
