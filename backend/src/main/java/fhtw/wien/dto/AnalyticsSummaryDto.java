package fhtw.wien.dto;

/**
 * DTO for overall analytics summary.
 */
public record AnalyticsSummaryDto(
        long totalDocuments,
        long highQualityDocuments,
        double averageQualityScore,
        long totalPagesProcessed,
        double averageProcessingTimeMs,
        long totalCharactersProcessed,
        long totalWordsProcessed
) {
}
