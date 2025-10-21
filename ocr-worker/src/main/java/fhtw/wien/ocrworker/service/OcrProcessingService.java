package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.dto.DocumentResponse;
import fhtw.wien.ocrworker.dto.OcrAcknowledgment;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing OCR operations asynchronously.
 * Integrates with Tesseract OCR for real text extraction from documents.
 */
@Service
public class OcrProcessingService {
    
    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);
    
    private final UnifiedOcrService unifiedOcrService;
    
    public OcrProcessingService(UnifiedOcrService unifiedOcrService) {
        this.unifiedOcrService = unifiedOcrService;
    }
    
    /**
     * Processes a document asynchronously for OCR using Tesseract.
     * Downloads document from MinIO, performs OCR extraction, and returns results.
     * 
     * @param document the document to process
     * @return a CompletableFuture containing the processing result
     */
    @Async
    public CompletableFuture<OcrAcknowledgment> processDocument(DocumentResponse document) {
        log.info("🔄 Starting real OCR processing for document: {} ('{}'')", 
                document.id(), document.title());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Perform actual OCR processing
                OcrResultDto ocrResult = unifiedOcrService.processDocument(document);
                
                // Convert OCR result to acknowledgment
                if (ocrResult.isSuccess()) {
                    log.info("✅ OCR processing completed successfully for document: {}", document.id());
                    return createSuccessAcknowledgment(document, ocrResult);
                } else {
                    log.warn("⚠️ OCR processing failed for document: {} - {}", document.id(), ocrResult.errorMessage());
                    return createFailureAcknowledgment(document, ocrResult.errorMessage());
                }
                
            } catch (Exception e) {
                log.error("❌ Unexpected error during OCR processing for document: {}", document.id(), e);
                return createFailureAcknowledgment(document, "Unexpected error: " + e.getMessage());
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("❌ OCR processing failed for document: {}", document.id(), throwable);
            } else {
                log.info("✅ OCR processing completed for document: {} with status: {}", 
                        document.id(), result.status());
            }
        });
    }
    
    /**
     * Creates a success acknowledgment from OCR result.
     */
    private OcrAcknowledgment createSuccessAcknowledgment(DocumentResponse document, OcrResultDto ocrResult) {
        String message = String.format("OCR completed: %d characters extracted from %d pages (confidence: %d%%, time: %dms)",
                ocrResult.totalCharacters(), ocrResult.totalPages(), ocrResult.overallConfidence(), ocrResult.processingTimeMs());
        
        return new OcrAcknowledgment(
                document.id(),
                document.title(),
                "SUCCESS",
                message,
                Instant.now()
        );
    }
    
    /**
     * Creates a failure acknowledgment.
     */
    private OcrAcknowledgment createFailureAcknowledgment(DocumentResponse document, String message) {
        return new OcrAcknowledgment(
                document.id(),
                document.title(),
                "FAILED",
                message,
                Instant.now()
        );
    }
}