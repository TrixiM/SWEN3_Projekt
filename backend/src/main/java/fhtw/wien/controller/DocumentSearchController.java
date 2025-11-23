package fhtw.wien.controller;

import fhtw.wien.dto.DocumentSearchDto;
import fhtw.wien.service.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/v1/documents")
@CrossOrigin(origins = "*")
@Tag(name = "Document Search", description = "Endpoints for searching documents using full-text search")
public class DocumentSearchController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchController.class);
    
    private final DocumentSearchService searchService;
    
    public DocumentSearchController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }
    

    @GetMapping("/search")
    @Operation(summary = "Search documents", 
               description = "Search documents by query string in both title and content fields")
    public ResponseEntity<List<DocumentSearchDto>> searchDocuments(
            @Parameter(description = "Search query string", required = true)
            @RequestParam String q) {
        
        log.info("📥 Search request received for query: '{}'", q);
        
        if (!isValidQuery(q)) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<DocumentSearchDto> results = searchService.search(q);
            log.info("✅ Search completed, found {} results", results.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("❌ Search failed for query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/fuzzy")
    @Operation(summary = "Fuzzy search documents",
               description = "Fuzzy search documents in both title and content fields. Handles typos and misspellings. "
                           + "Fuzziness values: 0 (exact), 1 (1 char difference), 2 (2 chars), AUTO (recommended)")
    public ResponseEntity<List<DocumentSearchDto>> fuzzySearchDocuments(
            @Parameter(description = "Search query string", required = true)
            @RequestParam String q,
            @Parameter(description = "Fuzziness level (0, 1, 2, or AUTO)", example = "AUTO")
            @RequestParam(defaultValue = "AUTO") String fuzziness) {
        
        log.info("📥 Fuzzy search request received for query: '{}' with fuzziness: {}", q, fuzziness);
        
        if (!isValidQuery(q)) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<DocumentSearchDto> results = searchService.fuzzySearch(q, fuzziness);
            log.info("✅ Fuzzy search completed, found {} results", results.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("❌ Fuzzy search failed for query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    private boolean isValidQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query received");
            return false;
        }
        return true;
    }
}
