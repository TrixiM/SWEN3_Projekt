package fhtw.wien.genaiworker.dto;

import java.time.Instant;
import java.util.UUID;

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

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
