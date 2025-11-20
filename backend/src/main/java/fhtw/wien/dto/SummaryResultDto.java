package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;


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
