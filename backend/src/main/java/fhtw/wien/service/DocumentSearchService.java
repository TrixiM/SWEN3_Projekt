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
        // Delegate to fuzzy search with AUTO fuzziness so that plain /search is always
        // full-text and typo-tolerant as well.
        log.info("🔍 Delegating exact search to fuzzy search for query: '{}'", queryString);
        return fuzzySearch(queryString, "AUTO");
    }

    

    public List<DocumentSearchDto> fuzzySearch(String queryString, String fuzziness) {
        log.info("🔍 Fuzzy searching documents for: '{}' with fuzziness: {}", queryString, fuzziness);
        
        try {
            // Build fuzzy queries with field boosting (title is 2x more important than content)
            Query titleFuzzy = FuzzyQuery.of(f -> f
                    .field("title")
                    .value(queryString)
                    .fuzziness(fuzziness)
                    .boost(2.0f))._toQuery();
            
            Query contentFuzzy = FuzzyQuery.of(f -> f
                    .field("content")
                    .value(queryString)
                    .fuzziness(fuzziness)
                    .boost(1.0f))._toQuery();
            
            Query boolQuery = BoolQuery.of(b -> b
                    .should(titleFuzzy)
                    .should(contentFuzzy))._toQuery();
            
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(boolQuery)
                    // Fetch all fields including content for snippet generation
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
