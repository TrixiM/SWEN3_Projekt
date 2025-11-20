package fhtw.wien.ocrworker.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Elasticsearch document indexing operations in ocr-worker.
 * Used for WRITE operations (indexing documents after OCR processing).
 * 
 * Note: backend has DocumentSearchRepository for READ operations.
 */
@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {
    
    /**
     * Find document by document ID.
     */
    DocumentIndex findByDocumentId(UUID documentId);
}
