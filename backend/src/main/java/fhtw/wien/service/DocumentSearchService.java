package fhtw.wien.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import fhtw.wien.dto.DocumentSearchDto;
import fhtw.wien.elasticsearch.DocumentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class DocumentSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    
    private final ElasticsearchOperations elasticsearchOperations;
    private static final Set<String> FILE_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");



    public DocumentSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }
    
 
     public List<DocumentSearchDto> search(String queryString) {

         log.info("🔍 Delegating exact search to fuzzy search for query: '{}'", queryString);
         return fuzzySearch(queryString, "AUTO");
     }

    

    public List<DocumentSearchDto> fuzzySearch(String queryString, String fuzziness) {
        log.info("🔍 Fuzzy searching documents for: '{}' with fuzziness: {}", queryString, fuzziness);
        
        try {

            Query searchQuery;
            if (looksLikeFilename(queryString)) {
                searchQuery = MultiMatchQuery.of(t -> t.fields("title.keyword").query(queryString).fuzziness("AUTO"))._toQuery();
            }else {
                searchQuery = MultiMatchQuery.of(q -> q
                        .query(queryString)
                        .fields("title^2", "content")      // boost title higher
                        .type(TextQueryType.BestFields)
                        .operator(Operator.And)             // or And for stricter matches
                        .fuzziness("AUTO")                 // AUTO applies fuzziness depending on term length
                        .maxExpansions(50)                 // limit performance impact
                        .minimumShouldMatch("70%")         // avoid overly broad matches
                )._toQuery();

            }

            NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(searchQuery)
            // Optional: highlight to produce snippets
            //.withHighlightQuery(new HighlightQuery(new Highlight(fieldOptions), DocumentIndex.class))
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

    private boolean looksLikeFilename(String input) {
        // must contain exactly one dot
        long dotCount = input.chars().filter(c -> c == '.').count();
        if (dotCount == 0) return false;

        int idx = input.lastIndexOf(".");
        if (idx <= 0 || idx == input.length() - 1) return false;

        String ext = input.substring(idx + 1).toLowerCase();
        return FILE_EXTENSIONS.contains(ext);
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
