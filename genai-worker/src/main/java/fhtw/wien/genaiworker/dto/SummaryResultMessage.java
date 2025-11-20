package fhtw.wien.genaiworker.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Message sent after generating a document summary.
 * Contains the generated summary and processing metadata.
 */
public record SummaryResultMessage(
        String messageId,
        UUID documentId,
        String title,
        String summary,
        String status,
        String errorMessage,
        long processingTimeMs,
        Instant timestamp
) {
    
    /**
     * Creates a successful summary result.
     */
    public static SummaryResultMessage success(UUID documentId, String title, String summary, long processingTimeMs) {
        return new SummaryResultMessage(
                UUID.randomUUID().toString(),
                documentId,
                title,
                summary,
                "SUCCESS",
                null,
                processingTimeMs,
                Instant.now()
        );
    }
    
    /**
     * Creates a failed summary result.
     */
    public static SummaryResultMessage failure(UUID documentId, String title, String errorMessage, long processingTimeMs) {
        return new SummaryResultMessage(
                UUID.randomUUID().toString(),
                documentId,
                title,
                null,
                "FAILED",
                errorMessage,
                processingTimeMs,
                Instant.now()
        );
    }
    
    /**
     * Checks if summary generation was successful.
     */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
