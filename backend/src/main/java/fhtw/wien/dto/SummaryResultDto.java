package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;

// TODO: This DTO is duplicated in genai-worker (SummaryResultMessage.java)
// Consider creating a shared-dtos module to eliminate duplication
// Any changes here MUST be synchronized with genai-worker/dto/SummaryResultMessage.java
public record SummaryResultDto(
        String messageId,
        UUID documentId,
        String title,
        String summary,
        Status status,
        String errorMessage,
        long processingTimeMs,
        Instant timestamp
) {
    public enum Status {
        SUCCESS, FAILED, PENDING
    }
    
    public static SummaryResultDto success(UUID documentId, String title, String summary, long processingTimeMs) {
        return new SummaryResultDto(
                UUID.randomUUID().toString(),
                documentId,
                title,
                summary,
                Status.SUCCESS,
                null,
                processingTimeMs,
                Instant.now()
        );
    }
    
    public static SummaryResultDto failure(UUID documentId, String title, String errorMessage, long processingTimeMs) {
        return new SummaryResultDto(
                UUID.randomUUID().toString(),
                documentId,
                title,
                null,
                Status.FAILED,
                errorMessage,
                processingTimeMs,
                Instant.now()
        );
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
