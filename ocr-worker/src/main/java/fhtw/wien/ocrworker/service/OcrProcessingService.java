package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.dto.DocumentResponse;
import fhtw.wien.ocrworker.dto.OcrAcknowledgment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for processing OCR operations asynchronously.
 * Replaces Thread.sleep with proper async processing and configurable parameters.
 */
@Service
public class OcrProcessingService {
    
    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);
    
    @Value("${ocr.processing.min-delay:500}")
    private int minProcessingDelay;
    
    @Value("${ocr.processing.max-delay:3000}")
    private int maxProcessingDelay;
    
    @Value("${ocr.processing.success-rate:0.95}")
    private double successRate;
    
    /**
     * Processes a document asynchronously for OCR.
     * In a real implementation, this would integrate with OCR libraries like Tesseract.
     * 
     * @param document the document to process
     * @return a CompletableFuture containing the processing result
     */
    @Async
    public CompletableFuture<OcrAcknowledgment> processDocument(DocumentResponse document) {
        log.info("🔄 Starting OCR processing for document: {} ('{}')", 
                document.id(), document.title());
        
        try {
            // Simulate variable processing time based on document characteristics
            int processingDelay = calculateProcessingDelay(document);
            
            log.debug("Estimated processing time: {}ms for document: {}", 
                    processingDelay, document.id());
            
            // Use CompletableFuture.delayedExecutor instead of Thread.sleep
            return CompletableFuture
                    .supplyAsync(() -> performOcrProcessing(document), 
                            CompletableFuture.delayedExecutor(processingDelay, 
                                    java.util.concurrent.TimeUnit.MILLISECONDS))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("❌ OCR processing failed for document: {}", 
                                    document.id(), throwable);
                        } else {
                            log.info("✅ OCR processing completed for document: {} with status: {}", 
                                    document.id(), result.status());
                        }
                    });
                    
        } catch (Exception e) {
            log.error("❌ Error starting OCR processing for document: {}", document.id(), e);
            return CompletableFuture.completedFuture(
                    createFailureAcknowledgment(document, "Failed to start OCR processing: " + e.getMessage())
            );
        }
    }
    
    /**
     * Performs the actual OCR processing (simulated).
     * In a real implementation, this would call OCR libraries.
     */
    private OcrAcknowledgment performOcrProcessing(DocumentResponse document) {
        try {
            // Simulate processing based on configurable success rate
            boolean isSuccess = ThreadLocalRandom.current().nextDouble() < successRate;
            
            if (isSuccess) {
                log.info("✅ OCR processing completed successfully for document: {}", document.id());
                return createSuccessAcknowledgment(document);
            } else {
                log.warn("⚠️ OCR processing failed (simulated failure) for document: {}", document.id());
                return createFailureAcknowledgment(document, "OCR processing failed (simulated)");
            }
            
        } catch (Exception e) {
            log.error("❌ Unexpected error during OCR processing for document: {}", document.id(), e);
            return createFailureAcknowledgment(document, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Calculates processing delay based on document characteristics.
     * Larger documents take longer to process.
     */
    private int calculateProcessingDelay(DocumentResponse document) {
        // Base delay plus extra time based on document size
        int baseDelay = ThreadLocalRandom.current().nextInt(minProcessingDelay, maxProcessingDelay);
        
        // Add extra delay for larger documents (rough simulation)
        long sizeInMB = document.sizeBytes() / (1024 * 1024);
        int sizeDelay = (int) (sizeInMB * 100); // 100ms per MB
        
        return Math.min(baseDelay + sizeDelay, maxProcessingDelay);
    }
    
    /**
     * Creates a success acknowledgment.
     */
    private OcrAcknowledgment createSuccessAcknowledgment(DocumentResponse document) {
        return new OcrAcknowledgment(
                document.id(),
                document.title(),
                "SUCCESS",
                "Document processed successfully. Text extracted and indexed.",
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