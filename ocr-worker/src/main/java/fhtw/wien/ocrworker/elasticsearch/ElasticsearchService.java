package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for indexing and searching documents in Elasticsearch.
 */
@Service
public class ElasticsearchService {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);
    
    private final DocumentIndexRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    
    public ElasticsearchService(DocumentIndexRepository repository,
                               ElasticsearchOperations elasticsearchOperations) {
        this.repository = repository;
        this.elasticsearchOperations = elasticsearchOperations;
    }
    
    /**
     * Index OCR result into Elasticsearch.
     */
    public DocumentIndex indexDocument(OcrResultDto ocrResult) {
        log.info("📇 Indexing document {} into Elasticsearch", ocrResult.documentId());
        
        try {
            DocumentIndex documentIndex = new DocumentIndex(
                    ocrResult.documentId(),
                    ocrResult.documentTitle(),
                    ocrResult.extractedText(),
                    ocrResult.totalCharacters(),
                    ocrResult.totalPages(),
                    ocrResult.language(),
                    ocrResult.overallConfidence(),
                    ocrResult.processedAt()
            );
            
            DocumentIndex saved = repository.save(documentIndex);
            log.info("✅ Successfully indexed document {} with {} characters", 
                    ocrResult.documentId(), ocrResult.totalCharacters());
            
            return saved;
        } catch (Exception e) {
            log.error("❌ Failed to index document {}: {}", ocrResult.documentId(), e.getMessage(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * Search documents by query string (searches in content and title).
     */
    public List<DocumentIndex> search(String queryString) {
        log.info("🔍 Searching for: '{}'", queryString);
        
        try {
            // Create a criteria query that searches in both content and title
            Criteria criteria = new Criteria("content").contains(queryString)
                    .or("title").contains(queryString);
            
            Query query = new CriteriaQuery(criteria);
            
            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(query, DocumentIndex.class);
            List<DocumentIndex> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
            
            log.info("✅ Found {} results for query: '{}'", results.size(), queryString);
            return results;
            
        } catch (Exception e) {
            log.error("❌ Search failed for query '{}': {}", queryString, e.getMessage(), e);
            throw new RuntimeException("Search failed", e);
        }
    }
    
    /**
     * Find document by document ID.
     */
    public DocumentIndex findByDocumentId(UUID documentId) {
        log.info("🔍 Finding document by ID: {}", documentId);
        return repository.findByDocumentId(documentId);
    }
    
    /**
     * Delete document from index.
     */
    public void deleteDocument(UUID documentId) {
        log.info("🗑️ Deleting document {} from Elasticsearch", documentId);
        try {
            repository.deleteById(documentId.toString());
            log.info("✅ Successfully deleted document {}", documentId);
        } catch (Exception e) {
            log.error("❌ Failed to delete document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
}
