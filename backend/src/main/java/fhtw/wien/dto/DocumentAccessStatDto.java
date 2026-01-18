package fhtw.wien.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

//This DTO also exists in document access batch service
public record DocumentAccessStatDto(
        String messageId,
        UUID documentId,
        int accessCount,
        LocalDate date
) {

}
