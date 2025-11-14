package fhtw.wien.ocrworker.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Elasticsearch document operations.
 */
@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {
    
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
