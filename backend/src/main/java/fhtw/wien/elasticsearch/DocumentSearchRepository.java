package fhtw.wien.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Elasticsearch document search operations in the backend.
 * Used exclusively for READ operations (searching documents).
 * 
 * Note: ocr-worker has DocumentIndexRepository for WRITE operations.
 */
@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentIndex, String> {
    
    /**
     * Find document by document ID.
     */
    DocumentIndex findByDocumentId(UUID documentId);
    
    /**
     * Search documents by content containing query string.
     */
    List<DocumentIndex> findByContentContaining(String query);
    
    /**
     * Search documents by title containing query string.
     */
    List<DocumentIndex> findByTitleContaining(String query);
}
