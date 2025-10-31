package fhtw.wien.ocrworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record OcrAcknowledgment(
        UUID documentId,
        String documentTitle,
        String status,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant processedAt
) {
}
