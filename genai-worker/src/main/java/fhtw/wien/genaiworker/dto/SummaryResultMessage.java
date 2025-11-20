package fhtw.wien.genaiworker.dto;

import java.time.Instant;
import java.util.UUID;

public record SummaryResultMessage(
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
    

    public static SummaryResultMessage success(UUID documentId, String title, String summary, long processingTimeMs) {
        return new SummaryResultMessage(
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
    

    public static SummaryResultMessage failure(UUID documentId, String title, String errorMessage, long processingTimeMs) {
        return new SummaryResultMessage(
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
