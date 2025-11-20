package fhtw.wien.dto;


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
