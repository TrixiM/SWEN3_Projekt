package fhtw.wien.ocrworker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/**
 * Utility class for detecting and validating file types for OCR processing.
 * Supports both magic number detection and content type analysis.
 */
@Component
public class FileTypeDetector {
    
    private static final Logger log = LoggerFactory.getLogger(FileTypeDetector.class);
    
    // Supported image file types for OCR
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/bmp", 
            "image/tiff", "image/tif", "image/webp"
    );
    
    // Supported document file types
    private static final Set<String> SUPPORTED_DOCUMENT_TYPES = Set.of(
            "application/pdf"
    );
    
    // Magic numbers for file type detection
    private static final byte[] PDF_SIGNATURE = "%PDF".getBytes();
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] BMP_SIGNATURE = {0x42, 0x4D};
    private static final byte[] TIFF_SIGNATURE_LE = {0x49, 0x49, 0x2A, 0x00}; // Little endian
    private static final byte[] TIFF_SIGNATURE_BE = {0x4D, 0x4D, 0x00, 0x2A}; // Big endian
    
    /**
     * Represents the file type and processing strategy.
     */
    public enum FileType {
        PDF("PDF Document", true),
        IMAGE("Image", false),
        UNSUPPORTED("Unsupported", false);
        
        private final String description;
        private final boolean requiresConversion;
        
        FileType(String description, boolean requiresConversion) {
            this.description = description;
            this.requiresConversion = requiresConversion;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean requiresConversion() {
            return requiresConversion;
        }
    }
    
    /**
     * Detects the file type from a file path.
     *
     * @param filePath the path to the file
     * @return the detected file type
     * @throws IOException if file cannot be read
     */
    public FileType detectFileType(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return detectFileType(inputStream);
        }
    }
    
    /**
     * Detects the file type from an input stream using magic number analysis.
     *
     * @param inputStream the input stream to analyze
     * @return the detected file type
     * @throws IOException if stream cannot be read
     */
    public FileType detectFileType(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8]; // Enough for most magic numbers
        int bytesRead = inputStream.read(buffer);
        
        if (bytesRead < 4) {
            log.warn("File too small to determine type (only {} bytes)", bytesRead);
            return FileType.UNSUPPORTED;
        }
        
        // Check PDF signature
        if (startsWith(buffer, PDF_SIGNATURE)) {
            log.debug("Detected PDF file");
            return FileType.PDF;
        }
        
        // Check PNG signature
        if (bytesRead >= 8 && startsWith(buffer, PNG_SIGNATURE)) {
            log.debug("Detected PNG image");
            return FileType.IMAGE;
        }
        
        // Check JPEG signature
        if (startsWith(buffer, JPEG_SIGNATURE)) {
            log.debug("Detected JPEG image");
            return FileType.IMAGE;
        }
        
        // Check BMP signature
        if (startsWith(buffer, BMP_SIGNATURE)) {
            log.debug("Detected BMP image");
            return FileType.IMAGE;
        }
        
        // Check TIFF signature (both endianness)
        if (startsWith(buffer, TIFF_SIGNATURE_LE) || startsWith(buffer, TIFF_SIGNATURE_BE)) {
            log.debug("Detected TIFF image");
            return FileType.IMAGE;
        }
        
        log.warn("Unknown file type, magic number: {}", 
                Arrays.toString(Arrays.copyOf(buffer, Math.min(bytesRead, 8))));
        return FileType.UNSUPPORTED;
    }
    
    /**
     * Validates if the content type is supported for OCR processing.
     *
     * @param contentType the MIME content type
     * @return true if supported, false otherwise
     */
    public boolean isSupportedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String normalizedType = contentType.toLowerCase().split(";")[0].trim();
        return SUPPORTED_IMAGE_TYPES.contains(normalizedType) || 
               SUPPORTED_DOCUMENT_TYPES.contains(normalizedType);
    }
    
    /**
     * Gets the file type from content type string.
     *
     * @param contentType the MIME content type
     * @return the corresponding file type
     */
    public FileType getFileTypeFromContentType(String contentType) {
        if (contentType == null) {
            return FileType.UNSUPPORTED;
        }
        
        String normalizedType = contentType.toLowerCase().split(";")[0].trim();
        
        if (SUPPORTED_DOCUMENT_TYPES.contains(normalizedType)) {
            return FileType.PDF;
        }
        
        if (SUPPORTED_IMAGE_TYPES.contains(normalizedType)) {
            return FileType.IMAGE;
        }
        
        return FileType.UNSUPPORTED;
    }
    
    /**
     * Validates file size against the maximum allowed size.
     *
     * @param fileSizeBytes the file size in bytes
     * @param maxSizeBytes the maximum allowed size in bytes
     * @return true if file size is acceptable
     */
    public boolean isValidFileSize(long fileSizeBytes, long maxSizeBytes) {
        if (fileSizeBytes <= 0) {
            log.warn("Invalid file size: {} bytes", fileSizeBytes);
            return false;
        }
        
        if (fileSizeBytes > maxSizeBytes) {
            log.warn("File too large: {} bytes (max: {} bytes)", fileSizeBytes, maxSizeBytes);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets a human-readable description of supported file types.
     *
     * @return description string
     */
    public String getSupportedTypesDescription() {
        return "Supported types: PDF documents, JPEG, PNG, BMP, TIFF images";
    }
    
    /**
     * Helper method to check if a byte array starts with a specific signature.
     *
     * @param data the data to check
     * @param signature the signature to look for
     * @return true if data starts with signature
     */
    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        
        return true;
    }
}