package fhtw.wien.messaging;

import fhtw.wien.config.MessagingConstants;
import fhtw.wien.service.DocumentAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumer for OCR completion messages to update document analytics.
 */
@Component
public class OcrCompletionConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(OcrCompletionConsumer.class);
    
    private final DocumentAnalyticsService analyticsService;
    
    public OcrCompletionConsumer(DocumentAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    @RabbitListener(queues = MessagingConstants.OCR_COMPLETED_QUEUE)
    public void handleOcrCompletion(Map<String, Object> ocrResult) {
        try {
            UUID documentId = UUID.fromString((String) ocrResult.get("documentId"));
            String status = (String) ocrResult.get("status");
            
            log.info("📥 OCR completion message received for document: {} with status: {}", 
                    documentId, status);
            
            // Only create analytics for successful OCR
            if ("SUCCESS".equals(status)) {
                String extractedText = (String) ocrResult.get("extractedText");
                int totalCharacters = (Integer) ocrResult.getOrDefault("totalCharacters", 0);
                int totalPages = (Integer) ocrResult.getOrDefault("totalPages", 0);
                int confidence = (Integer) ocrResult.getOrDefault("overallConfidence", 0);
                String language = (String) ocrResult.getOrDefault("language", "unknown");
                long processingTime = ((Number) ocrResult.getOrDefault("processingTimeMs", 0L)).longValue();
                
                // Calculate word count
                int totalWords = extractedText != null && !extractedText.isEmpty() 
                        ? extractedText.trim().split("\\s+").length 
                        : 0;
                
                // Create or update analytics
                analyticsService.createOrUpdateAnalytics(
                        documentId,
                        totalCharacters,
                        totalWords,
                        totalPages,
                        confidence,
                        language,
                        processingTime
                );
                
                log.info("✅ Analytics updated for document: {}", documentId);
            } else {
                log.warn("⚠️ Skipping analytics for failed OCR: {}", documentId);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to process OCR completion message: {}", e.getMessage(), e);
        }
    }
}
