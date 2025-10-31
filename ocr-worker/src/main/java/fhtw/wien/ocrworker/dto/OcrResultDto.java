package fhtw.wien.ocrworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data transfer object for OCR processing results.
 * Contains extracted text, confidence scores, and processing metadata.
 */
public record OcrResultDto(
        UUID documentId,
        String documentTitle,
        String extractedText,
        int totalCharacters,
        int totalPages,
        List<PageResult> pageResults,
        String language,
        int overallConfidence,
        boolean isHighConfidence,
        long processingTimeMs,
        String status,
        String errorMessage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant processedAt
) {
    
    /**
     * Represents OCR results for a single page.
     */
    public record PageResult(
            int pageNumber,
            String extractedText,
            int characterCount,
            int confidence,
            boolean isHighConfidence,
            long processingTimeMs
    ) {}
    
    /**
     * Creates a successful OCR result.
     *
     * @param documentId the document UUID
     * @param documentTitle the document title
     * @param extractedText the complete extracted text
     * @param pageResults results for individual pages
     * @param language the OCR language used
     * @param overallConfidence overall confidence score
     * @param processingTimeMs total processing time
     * @return successful OCR result DTO
     */
    public static OcrResultDto success(
            UUID documentId,
            String documentTitle,
            String extractedText,
            List<PageResult> pageResults,
            String language,
            int overallConfidence,
            long processingTimeMs) {
        
        return new OcrResultDto(
                documentId,
                documentTitle,
                extractedText,
                extractedText != null ? extractedText.length() : 0,
                pageResults != null ? pageResults.size() : 0,
                pageResults,
                language,
                overallConfidence,
                overallConfidence >= 70, // Threshold for high confidence
                processingTimeMs,
                "SUCCESS",
                null,
                Instant.now()
        );
    }
    
    /**
     * Creates a failed OCR result.
     *
     * @param documentId the document UUID
     * @param documentTitle the document title
     * @param errorMessage the error message
     * @param processingTimeMs processing time before failure
     * @return failed OCR result DTO
     */
    public static OcrResultDto failure(
            UUID documentId,
            String documentTitle,
            String errorMessage,
            long processingTimeMs) {
        
        return new OcrResultDto(
                documentId,
                documentTitle,
                null,
                0,
                0,
                null,
                null,
                0,
                false,
                processingTimeMs,
                "FAILED",
                errorMessage,
                Instant.now()
        );
    }
    
    /**
     * Creates a page result from Tesseract OCR result.
     *
     * @param pageNumber the page number (1-based)
     * @param ocrResult the Tesseract OCR result
     * @return page result DTO
     */
    public static PageResult fromTesseractResult(int pageNumber, 
                                               String extractedText,
                                               int confidence,
                                               long processingTimeMs) {
        return new PageResult(
                pageNumber,
                extractedText != null ? extractedText.trim() : "",
                extractedText != null ? extractedText.trim().length() : 0,
                confidence,
                confidence >= 70, // Threshold for high confidence
                processingTimeMs
        );
    }
    
    /**
     * Checks if the OCR processing was successful.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Gets a summary of the OCR results.
     *
     * @return human-readable summary
     */
    public String getSummary() {
        if (isSuccess()) {
            return String.format("OCR Success: %d characters extracted from %d pages (confidence: %d%%, time: %dms)",
                    totalCharacters, totalPages, overallConfidence, processingTimeMs);
        } else {
            return String.format("OCR Failed: %s (time: %dms)", errorMessage, processingTimeMs);
        }
    }
}