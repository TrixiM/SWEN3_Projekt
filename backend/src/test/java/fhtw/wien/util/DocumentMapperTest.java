package fhtw.wien.util;

import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentStatus;
import fhtw.wien.dto.DocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMapperTest {

    private Document testDocument;
    private UUID testId;
    private Instant createdAt;
    private Instant updatedAt;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();

        testDocument = new Document(
                "Test Document",
                "test.pdf",
                "application/pdf",
                1024L,
                "test-bucket",
                "test-object-key",
                "s3://test-bucket/test-object-key",
                "checksum123456"
        );
        testDocument.setId(testId);
        testDocument.setStatus(DocumentStatus.UPLOADED);
        testDocument.setTags(List.of("tag1", "tag2", "tag3"));
        testDocument.setVersion(1);
        testDocument.setCreatedAt(createdAt);
        testDocument.setUpdatedAt(updatedAt);
    }

    @Test
    void toResponse_WithCompleteDocument_ShouldMapAllFields() {
        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        assertNotNull(response);
        assertEquals(testId, response.id());
        assertEquals("Test Document", response.title());
        assertEquals("test.pdf", response.originalFilename());
        assertEquals("application/pdf", response.contentType());
        assertEquals(1024L, response.sizeBytes());
        assertEquals("test-bucket", response.bucket());
        assertEquals("test-object-key", response.objectKey());
        assertEquals("s3://test-bucket/test-object-key", response.storageUri());
        assertEquals("checksum123456", response.checksumSha256());
        assertEquals(DocumentStatus.UPLOADED, response.status());
        assertNotNull(response.tags());
        assertEquals(3, response.tags().size());
        assertTrue(response.tags().contains("tag1"));
        assertTrue(response.tags().contains("tag2"));
        assertTrue(response.tags().contains("tag3"));
        assertEquals(1, response.version());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void toResponse_WithNullDocument_ShouldReturnNull() {
        DocumentResponse response = DocumentMapper.toResponse(null);

        assertNull(response);
    }

    @Test
    void toResponse_WithMinimalDocument_ShouldMapRequiredFields() {
        Document minimal = new Document(
                "Minimal Doc",
                "minimal.pdf",
                "application/pdf",
                512L,
                "bucket",
                "key",
                "uri",
                null
        );
        minimal.setId(testId);

        DocumentResponse response = DocumentMapper.toResponse(minimal);

        assertNotNull(response);
        assertEquals(testId, response.id());
        assertEquals("Minimal Doc", response.title());
        assertEquals("minimal.pdf", response.originalFilename());
        assertEquals("application/pdf", response.contentType());
        assertEquals(512L, response.sizeBytes());
        assertEquals("bucket", response.bucket());
        assertEquals("key", response.objectKey());
        assertEquals("uri", response.storageUri());
        assertNull(response.checksumSha256());
    }

    @Test
    void toResponse_WithEmptyTags_ShouldMapEmptyList() {
        testDocument.setTags(List.of());

        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        assertNotNull(response);
        assertNotNull(response.tags());
        assertTrue(response.tags().isEmpty());
    }

    @Test
    void toResponse_WithNullChecksum_ShouldHandleGracefully() {
        testDocument.setChecksumSha256(null);

        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        assertNotNull(response);
        assertNull(response.checksumSha256());
    }

    @Test
    void toResponse_WithDifferentStatuses_ShouldMapCorrectly() {
        DocumentStatus[] statuses = DocumentStatus.values();

        for (DocumentStatus status : statuses) {
            testDocument.setStatus(status);
            DocumentResponse response = DocumentMapper.toResponse(testDocument);

            assertNotNull(response);
            assertEquals(status, response.status());
        }
    }

    @Test
    void toResponse_ShouldCreateImmutableDTO() {
        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        // Verify it's a record (immutable by nature)
        assertNotNull(response);
        assertEquals(testDocument.getId(), response.id());

        // Modify original document
        testDocument.setTitle("Modified Title");

        // Response should remain unchanged
        assertEquals("Test Document", response.title());
    }

    @Test
    void toResponse_WithLongTitle_ShouldHandleCorrectly() {
        String longTitle = "A".repeat(255);
        testDocument.setTitle(longTitle);

        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        assertNotNull(response);
        assertEquals(longTitle, response.title());
    }

    @Test
    void toResponse_WithSpecialCharactersInFilename_ShouldMapCorrectly() {
        testDocument.setOriginalFilename("test-file_v2.0 (final).pdf");

        DocumentResponse response = DocumentMapper.toResponse(testDocument);

        assertNotNull(response);
        assertEquals("test-file_v2.0 (final).pdf", response.originalFilename());
    }
}
