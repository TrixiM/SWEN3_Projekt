package fhtw.wien.service;

import fhtw.wien.dto.DocumentSearchDto;
import fhtw.wien.elasticsearch.DocumentIndex;
import fhtw.wien.elasticsearch.DocumentSearchRepository;
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
import java.util.stream.Collectors;

/**
 * Service for searching documents using Elasticsearch.
 */
@Service
public class DocumentSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    
    private final DocumentSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    
    public DocumentSearchService(DocumentSearchRepository repository,
                                ElasticsearchOperations elasticsearchOperations) {
        this.repository = repository;
        this.elasticsearchOperations = elasticsearchOperations;
    }
    
    /**
     * Search documents by query string (searches in content and title).
     */
    public List<DocumentSearchDto> search(String queryString) {
        log.info("🔍 Searching documents for: '{}'", queryString);
        
        try {
            // Create a criteria query that searches in both content and title
            Criteria criteria = new Criteria("content").contains(queryString)
                    .or("title").contains(queryString);
            
            Query query = new CriteriaQuery(criteria);
            
            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(query, DocumentIndex.class);
            List<DocumentSearchDto> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .map(doc -> DocumentSearchDto.from(
                            doc.getDocumentId(),
                            doc.getTitle(),
                            doc.getContent(),
                            doc.getTotalCharacters(),
                            doc.getTotalPages(),
                            doc.getLanguage(),
                            doc.getConfidence(),
                            doc.getIndexedAt(),
                            doc.getProcessedAt()
                    ))
                    .collect(Collectors.toList());
            
            log.info("✅ Found {} results for query: '{}'", results.size(), queryString);
            return results;
            
        } catch (Exception e) {
            log.error("❌ Search failed for query '{}': {}", queryString, e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search documents by title only.
     */
    public List<DocumentSearchDto> searchByTitle(String title) {
        log.info("🔍 Searching documents by title: '{}'", title);
        
        try {
            List<DocumentIndex> results = repository.findByTitleContaining(title);
            
            return results.stream()
                    .map(doc -> DocumentSearchDto.from(
                            doc.getDocumentId(),
                            doc.getTitle(),
                            doc.getContent(),
                            doc.getTotalCharacters(),
                            doc.getTotalPages(),
                            doc.getLanguage(),
                            doc.getConfidence(),
                            doc.getIndexedAt(),
                            doc.getProcessedAt()
                    ))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Title search failed for '{}': {}", title, e.getMessage(), e);
            throw new RuntimeException("Title search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search documents by content only.
     */
    public List<DocumentSearchDto> searchByContent(String content) {
        log.info("🔍 Searching documents by content: '{}'", content);
        
        try {
            List<DocumentIndex> results = repository.findByContentContaining(content);
            
            return results.stream()
                    .map(doc -> DocumentSearchDto.from(
                            doc.getDocumentId(),
                            doc.getTitle(),
                            doc.getContent(),
                            doc.getTotalCharacters(),
                            doc.getTotalPages(),
                            doc.getLanguage(),
                            doc.getConfidence(),
                            doc.getIndexedAt(),
                            doc.getProcessedAt()
                    ))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Content search failed for '{}': {}", content, e.getMessage(), e);
            throw new RuntimeException("Content search failed: " + e.getMessage(), e);
        }
    }
}
