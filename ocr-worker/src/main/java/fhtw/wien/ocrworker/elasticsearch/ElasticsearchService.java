package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for indexing documents in Elasticsearch.
 * Search functionality is handled by the backend service.
 */
@Service
public class ElasticsearchService {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);
    
    private final DocumentIndexRepository repository;
    
    public ElasticsearchService(DocumentIndexRepository repository) {
        this.repository = repository;
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
