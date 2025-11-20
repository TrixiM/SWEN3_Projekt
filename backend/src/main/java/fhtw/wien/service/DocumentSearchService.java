package fhtw.wien.service;

import fhtw.wien.dto.DocumentSearchDto;
import fhtw.wien.elasticsearch.DocumentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class DocumentSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    public DocumentSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }
    

    public List<DocumentSearchDto> search(String queryString) {
        log.info("🔍 Searching documents for: '{}'", queryString);
        
        try {
            // Create a criteria query that searches in both content and title
            Criteria criteria = new Criteria("content").contains(queryString)
                    .or("title").contains(queryString);
            
            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
            criteriaQuery.addSourceFilter(new FetchSourceFilter(new String[]{"documentId", "title", "totalCharacters", 
                            "totalPages", "language", "confidence", "indexedAt", "processedAt"}, 
                            new String[]{"content"}));
            
            org.springframework.data.elasticsearch.core.query.Query query = criteriaQuery;
            
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

    

    public List<DocumentSearchDto> fuzzySearch(String queryString, String fuzziness) {
        log.info("🔍 Fuzzy searching documents for: '{}' with fuzziness: {}", queryString, fuzziness);
        
        try {
            // Build Elasticsearch fuzzy query using native query builders
            Query contentFuzzy = FuzzyQuery.of(f -> f
                    .field("content")
                    .value(queryString)
                    .fuzziness(fuzziness))._toQuery();
            
            Query titleFuzzy = FuzzyQuery.of(f -> f
                    .field("title")
                    .value(queryString)
                    .fuzziness(fuzziness))._toQuery();
            
            Query boolQuery = BoolQuery.of(b -> b
                    .should(contentFuzzy)
                    .should(titleFuzzy))._toQuery();
            
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(boolQuery)
                    .withSourceFilter(new FetchSourceFilter(new String[]{"documentId", "title", "totalCharacters", 
                            "totalPages", "language", "confidence", "indexedAt", "processedAt"}, 
                            new String[]{"content"}))
                    .build();
            
            SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(nativeQuery, DocumentIndex.class);
            
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

    public List<DocumentSearchDto> fuzzySearch(String queryString) {
        return fuzzySearch(queryString, "AUTO");
    }
    

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
}
