package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for receiving summary results from GenAI worker.
 */
public record SummaryResultDto(
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
