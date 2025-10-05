package fhtw.wien.ocrworker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fhtw.wien.ocrworker.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        String bucket,
        String objectKey,
        String storageUri,
        String checksumSha256,
        DocumentStatus status,
        Long version,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant updatedAt
) {
}
