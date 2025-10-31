package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for document analytics data.
 */
public record DocumentAnalyticsDto(
        UUID id,
        UUID documentId,
        int totalCharacters,
        int totalWords,
        int totalPages,
        int averageConfidence,
        String language,
        long ocrProcessingTimeMs,
        double wordCountPerPage,
        double characterCountPerPage,
        boolean isHighQuality,
        double qualityScore,
        Instant createdAt,
        Instant updatedAt
) {
}
