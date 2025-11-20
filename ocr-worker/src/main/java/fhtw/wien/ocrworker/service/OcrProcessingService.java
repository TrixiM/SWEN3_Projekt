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


@Service
public class OcrProcessingService {
    
    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);
    
    private final UnifiedOcrService unifiedOcrService;
    
    public OcrProcessingService(UnifiedOcrService unifiedOcrService) {
        this.unifiedOcrService = unifiedOcrService;
    }

    @Async
    public CompletableFuture<OcrResultDto> processDocument(DocumentResponse document) { //CompletableFuture represents a future result of an asynchronous computation
        log.info("🔄 Starting real OCR processing for document: {} ('{}'')",           // - essentially a placeholder for a value that will be computed and
                document.id(), document.title());                                      //available at some point in the future. It allows your application to
                                                                                        // continue executing other tasks while waiting for long-running operations
                                                                                        //to complete.


        
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
