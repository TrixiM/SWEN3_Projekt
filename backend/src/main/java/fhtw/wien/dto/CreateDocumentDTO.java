package fhtw.wien.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateDocumentDTO (
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 255) String originalFilename,
    @NotBlank @Size(max = 127) String contentType,
    @Positive long sizeBytes,
    @NotBlank @Size(max = 63) String bucket,
    @NotBlank String objectKey,
    String checksumSha256,
    List<String> tags
) {}
