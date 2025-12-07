package fhtw.wien.ocrworker.dto;
import java.util.UUID;

public record Document(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        String objectKey
) {
}
