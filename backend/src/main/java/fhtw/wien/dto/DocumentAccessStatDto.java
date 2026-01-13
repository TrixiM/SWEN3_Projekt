package fhtw.wien.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentAccessStatDto(
        String messageId,
        UUID documentId,
        int accessCount,
        Instant lastAccessedAt
) {

}
