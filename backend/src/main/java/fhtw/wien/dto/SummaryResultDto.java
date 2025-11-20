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
        String status,
        String errorMessage,
        long processingTimeMs,
        Instant timestamp
) {
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
