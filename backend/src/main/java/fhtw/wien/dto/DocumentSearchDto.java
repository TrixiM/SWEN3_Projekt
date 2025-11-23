package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;


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

    public static DocumentSearchDto from(UUID documentId, String title, String content, 
                                        int totalCharacters, int totalPages, String language, 
                                        int confidence, Instant indexedAt, Instant processedAt) {
        // Create a snippet from content if available (null when source filtering is applied)
        String snippet = null;
        if (content != null) {
            snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
        
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
