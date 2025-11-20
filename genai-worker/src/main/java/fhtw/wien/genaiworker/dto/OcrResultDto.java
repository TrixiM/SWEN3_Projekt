package fhtw.wien.genaiworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for receiving OCR results from OCR worker.
 * This matches the OcrResultDto from the OCR worker.
 */
public record OcrResultDto(
        String messageId,
        UUID documentId,
        String documentTitle,
        String extractedText,
        int totalCharacters,
        int totalPages,
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
     * Checks if OCR was successful.
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Checks if there is meaningful text to summarize.
     */
    public boolean hasValidText() {
        return extractedText != null && 
               !extractedText.trim().isEmpty() && 
               totalCharacters > 50;
    }
}
