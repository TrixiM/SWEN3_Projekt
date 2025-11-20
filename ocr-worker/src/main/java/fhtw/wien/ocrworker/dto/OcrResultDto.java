package fhtw.wien.ocrworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OcrResultDto(
        String messageId,
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

    public record PageResult(
            int pageNumber,
            String extractedText,
            int characterCount,
            int confidence,
            boolean isHighConfidence,
            long processingTimeMs
    ) {}

    public static OcrResultDto success(
            UUID documentId,
            String documentTitle,
            String extractedText,
            List<PageResult> pageResults,
            String language,
            int overallConfidence,
            long processingTimeMs) {
        
        return new OcrResultDto(
                UUID.randomUUID().toString(),
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

    public static OcrResultDto failure(
            UUID documentId,
            String documentTitle,
            String errorMessage,
            long processingTimeMs) {
        
        return new OcrResultDto(
                UUID.randomUUID().toString(),
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

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public String getSummary() {
        if (isSuccess()) {
            return String.format("OCR Success: %d characters extracted from %d pages (confidence: %d%%, time: %dms)",
                    totalCharacters, totalPages, overallConfidence, processingTimeMs);
        } else {
            return String.format("OCR Failed: %s (time: %dms)", errorMessage, processingTimeMs);
        }
    }
}