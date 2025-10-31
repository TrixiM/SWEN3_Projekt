package fhtw.wien.controller;

import fhtw.wien.dto.AnalyticsSummaryDto;
import fhtw.wien.dto.DocumentAnalyticsDto;
import fhtw.wien.service.DocumentAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for document analytics operations.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Document Analytics", description = "Endpoints for document analytics and statistics")
public class DocumentAnalyticsController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentAnalyticsController.class);
    
    private final DocumentAnalyticsService analyticsService;
    
    public DocumentAnalyticsController(DocumentAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    /**
     * Get analytics for a specific document.
     */
    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Get document analytics", 
               description = "Retrieve analytics data for a specific document")
    public ResponseEntity<DocumentAnalyticsDto> getDocumentAnalytics(
            @Parameter(description = "Document ID", required = true)
            @PathVariable UUID documentId) {
        
        log.info("📥 Analytics request for document: {}", documentId);
        
        return analyticsService.getAnalyticsByDocumentId(documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all high-quality documents.
     */
    @GetMapping("/high-quality")
    @Operation(summary = "Get high-quality documents", 
               description = "Retrieve all documents marked as high quality")
    public ResponseEntity<List<DocumentAnalyticsDto>> getHighQualityDocuments() {
        log.info("📥 Request for high-quality documents");
        List<DocumentAnalyticsDto> documents = analyticsService.getHighQualityDocuments();
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get documents by language.
     */
    @GetMapping("/language/{language}")
    @Operation(summary = "Get documents by language", 
               description = "Retrieve documents filtered by OCR language")
    public ResponseEntity<List<DocumentAnalyticsDto>> getDocumentsByLanguage(
            @Parameter(description = "Language code (e.g., 'eng', 'deu')", required = true)
            @PathVariable String language) {
        
        log.info("📥 Request for documents with language: {}", language);
        List<DocumentAnalyticsDto> documents = analyticsService.getDocumentsByLanguage(language);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get documents with minimum confidence.
     */
    @GetMapping("/confidence/{minConfidence}")
    @Operation(summary = "Get documents by minimum confidence", 
               description = "Retrieve documents with OCR confidence above threshold")
    public ResponseEntity<List<DocumentAnalyticsDto>> getDocumentsByConfidence(
            @Parameter(description = "Minimum confidence (0-100)", required = true)
            @PathVariable int minConfidence) {
        
        log.info("📥 Request for documents with confidence >= {}", minConfidence);
        
        if (minConfidence < 0 || minConfidence > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        List<DocumentAnalyticsDto> documents = analyticsService.getDocumentsByMinConfidence(minConfidence);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get overall analytics summary.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary", 
               description = "Retrieve overall analytics summary for all documents")
    public ResponseEntity<AnalyticsSummaryDto> getAnalyticsSummary() {
        log.info("📥 Request for analytics summary");
        AnalyticsSummaryDto summary = analyticsService.getAnalyticsSummary();
        log.info("✅ Analytics summary: {} total docs, avg quality: {}", 
                summary.totalDocuments(), summary.averageQualityScore());
        return ResponseEntity.ok(summary);
    }
}
