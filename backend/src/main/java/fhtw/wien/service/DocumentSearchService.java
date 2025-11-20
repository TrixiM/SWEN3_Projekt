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
import org.springframework.data.elasticsearch.core.query.StringQuery;
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
            List<DocumentIndex> documents = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
            
            List<DocumentSearchDto> results = mapToSearchDtos(documents);
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
            return mapToSearchDtos(results);
                    
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
            return mapToSearchDtos(results);
                    
        } catch (Exception e) {
            log.error("❌ Content search failed for '{}': {}", content, e.getMessage(), e);
            throw new RuntimeException("Content search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fuzzy search documents (handles typos and misspellings).
     * @param queryString the search query
     * @param fuzziness the edit distance (0-2, where AUTO is recommended)
     * @return list of matching documents
     */
    public List<DocumentSearchDto> fuzzySearch(String queryString, String fuzziness) {
        log.info("🔍 Fuzzy searching documents for: '{}' with fuzziness: {}", queryString, fuzziness);
        
        try {
            // Build Elasticsearch fuzzy query using query string DSL
            String queryJson = String.format(
                """
                {
                    "bool": {
                        "should": [
                            {
                                "fuzzy": {
                                    "content": {
                                        "value": "%s",
                                        "fuzziness": "%s"
                                    }
                                }
                            },
                            {
                                "fuzzy": {
                                    "title": {
                                        "value": "%s",
                                        "fuzziness": "%s"
                                    }
                                }
                            }
                        ]
                    }
                }
                """,
                escapeJson(queryString), fuzziness,
                escapeJson(queryString), fuzziness
            );
            
            Query query = new StringQuery(queryJson);
            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(query, DocumentIndex.class);
            
            List<DocumentIndex> documents = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
            
            List<DocumentSearchDto> results = mapToSearchDtos(documents);
            log.info("✅ Found {} fuzzy results for query: '{}'", results.size(), queryString);
            return results;
            
        } catch (Exception e) {
            log.error("❌ Fuzzy search failed for query '{}': {}", queryString, e.getMessage(), e);
            throw new RuntimeException("Fuzzy search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fuzzy search with default fuzziness (AUTO).
     */
    public List<DocumentSearchDto> fuzzySearch(String queryString) {
        return fuzzySearch(queryString, "AUTO");
    }
    
    /**
     * Maps a list of DocumentIndex to DocumentSearchDto.
     */
    private List<DocumentSearchDto> mapToSearchDtos(List<DocumentIndex> documents) {
        return documents.stream()
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
    }
    
    /**
     * Escape special JSON characters in query string.
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
