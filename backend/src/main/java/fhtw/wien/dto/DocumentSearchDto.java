package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for document search results.
 */
public record DocumentSearchDto(
        UUID documentId,
        String title,
        String contentSnippet,
        int totalCharacters,
        int totalPages,
        String language,
        int confidence,
        Instant indexedAt,
        Instant processedAt
) {
    /**
     * Creates a search result with a content snippet.
     */
    public static DocumentSearchDto from(UUID documentId, String title, String content, 
                                        int totalCharacters, int totalPages, String language, 
                                        int confidence, Instant indexedAt, Instant processedAt) {
        // Create a snippet of max 200 characters
        String snippet = content != null && content.length() > 200 
                ? content.substring(0, 200) + "..." 
                : content;
        
        return new DocumentSearchDto(
                documentId,
                title,
                snippet,
                totalCharacters,
                totalPages,
                language,
                confidence,
                indexedAt,
                processedAt
        );
    }
}
