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

/**
 * REST controller for document search operations using Elasticsearch.
 */
@RestController
@RequestMapping("/api/documents/search")
@Tag(name = "Document Search", description = "Endpoints for searching documents using full-text search")
public class DocumentSearchController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchController.class);
    
    private final DocumentSearchService searchService;
    
    public DocumentSearchController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }
    
    /**
     * Search documents by query string (searches in both title and content).
     */
    @GetMapping
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
    
    /**
     * Search documents by title only.
     */
    @GetMapping("/title")
    @Operation(summary = "Search documents by title", 
               description = "Search documents by title field only")
    public ResponseEntity<List<DocumentSearchDto>> searchByTitle(
            @Parameter(description = "Title search query", required = true)
            @RequestParam String q) {
        
        log.info("📥 Title search request received for query: '{}'", q);
        
        if (!isValidQuery(q)) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<DocumentSearchDto> results = searchService.searchByTitle(q);
            log.info("✅ Title search completed, found {} results", results.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("❌ Title search failed for query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search documents by content only.
     */
    @GetMapping("/content")
    @Operation(summary = "Search documents by content", 
               description = "Search documents by content field only")
    public ResponseEntity<List<DocumentSearchDto>> searchByContent(
            @Parameter(description = "Content search query", required = true)
            @RequestParam String q) {
        
        log.info("📥 Content search request received for query: '{}'", q);
        
        if (!isValidQuery(q)) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<DocumentSearchDto> results = searchService.searchByContent(q);
            log.info("✅ Content search completed, found {} results", results.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("❌ Content search failed for query '{}': {}", q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Fuzzy search documents (handles typos and misspellings).
     */
    @GetMapping("/fuzzy")
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
    
    /**
     * Validates search query string.
     */
    private boolean isValidQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query received");
            return false;
        }
        return true;
    }
}
