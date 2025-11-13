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
 * Uses Spring's @Async to handle document processing in background threads.
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
     * Spring's @Async annotation automatically wraps this in a CompletableFuture.
     * 
     * @param document the document to process
     * @return a CompletableFuture containing the OCR result
     */
    @Async
    public CompletableFuture<OcrResultDto> processDocument(DocumentResponse document) {
        log.info("🔄 Starting OCR processing for document: {} ('{}'')",
                document.id(), document.title());
        
        try {
            OcrResultDto ocrResult = unifiedOcrService.processDocument(document);
            
            if (ocrResult.isSuccess()) {
                log.info("✅ OCR processing completed successfully for document: {}", document.id());
            } else {
                log.warn("⚠️ OCR processing failed for document: {} - {}", 
                        document.id(), ocrResult.errorMessage());
            }
            
            return CompletableFuture.completedFuture(ocrResult);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error during OCR processing for document: {}", document.id(), e);
            OcrResultDto failureResult = OcrResultDto.failure(
                    document.id(), 
                    document.title(), 
                    "Unexpected error: " + e.getMessage(), 
                    0L
            );
            return CompletableFuture.completedFuture(failureResult);
        }
    }
    
}
