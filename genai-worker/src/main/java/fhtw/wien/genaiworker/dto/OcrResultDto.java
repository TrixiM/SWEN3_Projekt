package fhtw.wien.genaiworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean minimumTotalCharactersForSummary() {
        return totalCharacters >= 50;
    }

    public boolean hasValidText() {
        return extractedText != null &&
                !extractedText.trim().isEmpty() &&
                totalCharacters > 0;
    }
}
