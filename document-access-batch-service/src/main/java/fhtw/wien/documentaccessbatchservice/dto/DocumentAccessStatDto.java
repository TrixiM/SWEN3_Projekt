package fhtw.wien.documentaccessbatchservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DocumentAccessStatDto(
        String messageId,
        UUID documentId,
        LocalDate date,
        int accessCount
) {
    public DocumentAccessStatDto(UUID documentId, LocalDate date, int accessCount) {
        this(UUID.randomUUID().toString(), documentId, date, accessCount);
    }
}
