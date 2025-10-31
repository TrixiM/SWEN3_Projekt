package fhtw.wien.util;

import fhtw.wien.domain.Document;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Utility class for updating Document entities.
 * Provides safe field updates with null checks.
 */
public final class DocumentUpdateMapper {
    
    private DocumentUpdateMapper() {
        // Utility class
    }
    
    /**
     * Updates a document with non-null values from the update request.
     * 
     * @param existing the existing document to update
     * @param updateRequest the document containing update values
     */
    public static void updateDocument(Document existing, Document updateRequest) {
        updateFieldIfNotNull(updateRequest.getTitle(), existing::setTitle);
        updateFieldIfNotNull(updateRequest.getOriginalFilename(), existing::setOriginalFilename);
        updateFieldIfNotNull(updateRequest.getContentType(), existing::setContentType);
        updateFieldIfNotNull(updateRequest.getStatus(), existing::setStatus);
        updateFieldIfNotNull(updateRequest.getChecksumSha256(), existing::setChecksumSha256);
        updateFieldIfNotNull(updateRequest.getPdfData(), existing::setPdfData);
        updateFieldIfNotNull(updateRequest.getTags(), existing::setTags);
        
        // Always update the timestamp
        existing.setUpdatedAt(Instant.now());
    }
    
    /**
     * Updates a field if the new value is not null.
     * 
     * @param newValue the new value to set
     * @param setter the setter function to use
     * @param <T> the type of the field
     */
    private static <T> void updateFieldIfNotNull(T newValue, Consumer<T> setter) {
        if (newValue != null) {
            setter.accept(newValue);
        }
    }
}