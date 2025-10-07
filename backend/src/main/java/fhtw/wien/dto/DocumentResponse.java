package fhtw.wien.dto;

import fhtw.wien.domain.DocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String bucket,
        String objectKey,
        String storageUri,
        String checksumSha256,
        DocumentStatus status,
        List<String> tags,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}