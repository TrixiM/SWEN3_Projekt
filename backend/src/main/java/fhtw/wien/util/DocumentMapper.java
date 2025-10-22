package fhtw.wien.util;

import fhtw.wien.domain.Document;
import fhtw.wien.dto.DocumentResponse;

/**
 * Utility class for mapping between Document entity and DocumentResponse DTO.
 * Centralizes mapping logic to eliminate duplication.
 */
public final class DocumentMapper {
    
    private DocumentMapper() {
        // Utility class
    }
    
    /**
     * Maps a Document entity to a DocumentResponse DTO.
     * 
     * @param document the document entity to map
     * @return the mapped DocumentResponse DTO
     */
    public static DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }
        
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getBucket(),
                document.getObjectKey(),
                document.getStorageUri(),
                document.getChecksumSha256(),
                document.getStatus(),
                document.getTags(),
                document.getVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}