package fhtw.wien.repo;

import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced repository interface for Document entities with optimized queries.
 * Replaces the basic DocumentRepo with performance improvements and additional functionality.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Finds a document by its SHA-256 checksum.
     * Useful for detecting duplicate uploads.
     */
    Optional<Document> findByChecksumSha256(String checksumSha256);
    
    /**
     * Finds all documents with eager loading of tags to prevent N+1 queries.
     * Use this instead of findAll() when you need tags data.
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags")
    List<Document> findAllWithTags();
    
    /**
     * Finds all documents with pagination and eager loading of tags.
     */
    @Query(value = "SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.tags",
           countQuery = "SELECT COUNT(d) FROM Document d")
    Page<Document> findAllWithTags(Pageable pageable);
    
    /**
     * Finds a document by ID with eager loading of tags.
     * Use this when you need the document with its tags.
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags WHERE d.id = :id")
    Optional<Document> findByIdWithTags(@Param("id") UUID id);
    
    /**
     * Finds documents by status.
     */
    List<Document> findByStatus(DocumentStatus status);
    
    /**
     * Finds documents by status with pagination.
     */
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);
    
    /**
     * Finds documents by content type.
     */
    List<Document> findByContentType(String contentType);
    
    /**
     * Finds documents containing a specific tag.
     */
    @Query("SELECT d FROM Document d JOIN d.tags t WHERE t = :tag")
    List<Document> findByTag(@Param("tag") String tag);
    
    /**
     * Finds documents containing any of the specified tags.
     */
    @Query("SELECT DISTINCT d FROM Document d JOIN d.tags t WHERE t IN :tags")
    List<Document> findByTagsIn(@Param("tags") List<String> tags);
    
    /**
     * Finds documents by title containing the search term (case-insensitive).
     */
    List<Document> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Finds documents by original filename containing the search term (case-insensitive).
     */
    List<Document> findByOriginalFilenameContainingIgnoreCase(String filename);
    
    /**
     * Finds documents created after the specified date.
     */
    List<Document> findByCreatedAtAfter(Instant date);
    
    /**
     * Finds documents updated after the specified date.
     */
    List<Document> findByUpdatedAtAfter(Instant date);
    
    /**
     * Finds documents with size between the specified bounds.
     */
    List<Document> findBySizeBytesBetween(long minSize, long maxSize);
    
    /**
     * Full-text search across multiple fields.
     * Searches in title, filename, content type, and tags.
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN d.tags t WHERE " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.contentType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Document> searchDocuments(@Param("searchTerm") String searchTerm);
    
    /**
     * Full-text search with pagination.
     */
    @Query(value = "SELECT DISTINCT d FROM Document d LEFT JOIN d.tags t WHERE " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.contentType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           countQuery = "SELECT COUNT(DISTINCT d) FROM Document d LEFT JOIN d.tags t WHERE " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.contentType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Document> searchDocuments(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Counts documents by status.
     */
    long countByStatus(DocumentStatus status);
    
    /**
     * Checks if a document exists with the given checksum.
     * More efficient than loading the full document.
     */
    boolean existsByChecksumSha256(String checksumSha256);
    
    /**
     * Finds documents without PDF data (for cleanup operations).
     */
    @Query("SELECT d FROM Document d WHERE d.pdfData IS NULL")
    List<Document> findDocumentsWithoutPdfData();
    
    /**
     * Gets basic document info without loading large binary data.
     * Useful for listings where PDF data is not needed.
     */
    @Query("SELECT new fhtw.wien.domain.Document(d.id, d.title, d.originalFilename, d.contentType, " +
           "d.sizeBytes, d.bucket, d.objectKey, d.storageUri, d.checksumSha256, d.status, " +
           "d.version, d.createdAt, d.updatedAt) FROM Document d")
    List<Document> findAllBasicInfo();
    
    /**
     * Deletes documents older than the specified date.
     * Use with caution - typically for cleanup operations.
     */
    void deleteByCreatedAtBefore(Instant date);
}