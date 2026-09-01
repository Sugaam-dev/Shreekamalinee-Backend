package com.pmrgsolution.core.service;

import com.pmrgsolution.Exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Slf4j
@Service
public class ImageOptimizerServiceImpl implements ImageOptimizerService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final long MIN_FILE_SIZE_BYTES = 100; // 100 Bytes
    private static final int MAX_DIMENSION_LIMIT = 8000; // 8000px limit against decompression bombs
    private static final int MIN_DIMENSION_LIMIT = 50;   // 50px minimum
    private static final int TARGET_MAX_WIDTH_OR_HEIGHT = 1920; // Full HD target
    private static final float JPEG_COMPRESSION_QUALITY = 0.82f; // 82% quality (imperceptible loss)

    @Override
    public byte[] validateAndOptimizeImage(MultipartFile file) {
        // 1. Check file existence and size limits
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload an empty file.", HttpStatus.BAD_REQUEST);
        }

        long originalSize = file.getSize();
        if (originalSize > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("File size exceeds the 10 MB maximum limit.", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        if (originalSize < MIN_FILE_SIZE_BYTES) {
            throw new BusinessException("File is too small to be a valid image.", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] rawBytes = file.getBytes();

            // 2. Cryptographic Magic-Byte Validation (Inspect file headers)
            verifyMagicBytes(rawBytes);

            // 3. Decode Image into memory
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (originalImage == null) {
                throw new BusinessException("Corrupted image file. Unable to decode pixels.", HttpStatus.BAD_REQUEST);
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 4. Decompression bomb & dimension validation
            if (width > MAX_DIMENSION_LIMIT || height > MAX_DIMENSION_LIMIT) {
                throw new BusinessException(String.format("Image dimensions (%dx%d) exceed maximum allowed size of 8000x8000.", width, height), HttpStatus.BAD_REQUEST);
            }
            if (width < MIN_DIMENSION_LIMIT || height < MIN_DIMENSION_LIMIT) {
                throw new BusinessException(String.format("Image dimensions (%dx%d) are too small. Minimum required is 50x50.", width, height), HttpStatus.BAD_REQUEST);
            }

            // 5. Calculate target dimensions (scale down if > 1920px)
            int targetWidth = width;
            int targetHeight = height;

            if (width > TARGET_MAX_WIDTH_OR_HEIGHT || height > TARGET_MAX_WIDTH_OR_HEIGHT) {
                double scale = Math.min((double) TARGET_MAX_WIDTH_OR_HEIGHT / width, (double) TARGET_MAX_WIDTH_OR_HEIGHT / height);
                targetWidth = (int) Math.round(width * scale);
                targetHeight = (int) Math.round(height * scale);
            }

            // 6. Draw into RGB canvas with clean white background (handles transparent PNGs gracefully)
            BufferedImage optimizedCanvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = optimizedCanvas.createGraphics();

            try {
                // High-quality rendering hints (Bicubic downscaling, anti-aliasing)
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill background with white in case of transparent PNG/WebP
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetWidth, targetHeight);

                // Draw scaled image
                g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                g2d.dispose();
            }

            // 7. Compress to high-quality JPEG (stripping all EXIF/metadata)
            byte[] compressedBytes = compressToJpeg(optimizedCanvas, JPEG_COMPRESSION_QUALITY);

            long optimizedSize = compressedBytes.length;
            double reductionPercent = (1.0 - ((double) optimizedSize / originalSize)) * 100.0;
            log.info("Image Optimized: [{}x{} -> {}x{}] | Original: {} KB -> Optimized: {} KB ({:.1f}% reduction)",
                    width, height, targetWidth, targetHeight,
                    originalSize / 1024, optimizedSize / 1024, reductionPercent);

            return compressedBytes;

        } catch (IOException e) {
            log.error("Failed to process and optimize image: {}", e.getMessage(), e);
            throw new BusinessException("Image processing failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void verifyMagicBytes(byte[] bytes) {
        if (bytes.length < 8) {
            throw new BusinessException("Invalid file header.", HttpStatus.BAD_REQUEST);
        }
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;

        boolean isJpeg = (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF);
        boolean isPng = (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47);
        boolean isGif = (b0 == 0x47 && b1 == 0x49 && b2 == 0x46);
        boolean isWebP = (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46); // RIFF

        if (!isJpeg && !isPng && !isGif && !isWebP) {
            throw new BusinessException("Unsupported or fake file format. Allowed formats: JPEG, PNG, GIF, WebP.", HttpStatus.BAD_REQUEST);
        }
    }

    private byte[] compressToJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter found in JVM");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            JPEGImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
