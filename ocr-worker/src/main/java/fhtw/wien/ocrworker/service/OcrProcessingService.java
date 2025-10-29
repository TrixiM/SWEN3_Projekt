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
     * @return a CompletableFuture containing the OCR result
     */
    @Async
    public CompletableFuture<OcrResultDto> processDocument(DocumentResponse document) {
        log.info("🔄 Starting real OCR processing for document: {} ('{}'')", 
                document.id(), document.title());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Perform actual OCR processing
                OcrResultDto ocrResult = unifiedOcrService.processDocument(document);
                
                if (ocrResult.isSuccess()) {
                    log.info("✅ OCR processing completed successfully for document: {}", document.id());
                } else {
                    log.warn("⚠️ OCR processing failed for document: {} - {}", document.id(), ocrResult.errorMessage());
                }
                
                return ocrResult;
                
            } catch (Exception e) {
                log.error("❌ Unexpected error during OCR processing for document: {}", document.id(), e);
                return OcrResultDto.failure(document.id(), document.title(), "Unexpected error: " + e.getMessage(), 0L);
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
    
}
