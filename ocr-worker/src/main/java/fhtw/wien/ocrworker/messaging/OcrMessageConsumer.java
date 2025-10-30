package fhtw.wien.ocrworker.messaging;

import fhtw.wien.ocrworker.config.RabbitMQConfig;
import fhtw.wien.ocrworker.dto.DocumentResponse;
import fhtw.wien.ocrworker.dto.OcrAcknowledgment;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import fhtw.wien.ocrworker.service.IdempotencyService;
import fhtw.wien.ocrworker.service.OcrProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OcrMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final OcrProcessingService ocrProcessingService;
    private final IdempotencyService idempotencyService;

    public OcrMessageConsumer(RabbitTemplate rabbitTemplate, OcrProcessingService ocrProcessingService,
                              IdempotencyService idempotencyService) {
        this.rabbitTemplate = rabbitTemplate;
        this.ocrProcessingService = ocrProcessingService;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void handleDocumentCreated(DocumentResponse document) {
        log.info("📄 OCR WORKER RECEIVED: Document created - ID: {}, Title: '{}'",
                document.id(), document.title());
        
        // Idempotency check
        String messageId = "ocr-doc-" + document.id();
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("⏭️ Skipping duplicate document processing: {}", document.id());
            return;
        }
        
        log.info("📋 Document Details:");
        log.info("   - Filename: {}", document.originalFilename());
        log.info("   - Content Type: {}", document.contentType());
        log.info("   - Size: {} bytes", document.sizeBytes());
        log.info("   - Status: {}", document.status());
        
        // Process document using the improved async service
        log.info("🔄 Delegating OCR processing to service...");
        
        ocrProcessingService.processDocument(document)
                .whenComplete((ocrResult, throwable) -> {
                    if (throwable != null) {
                        log.error("❌ OCR processing failed for document: {}", document.id(), throwable);
                        // Send failure acknowledgment
                        OcrAcknowledgment failureAck = new OcrAcknowledgment(
                                document.id(),
                                document.title(),
                                "FAILED",
                                "Processing failed: " + throwable.getMessage(),
                                Instant.now()
                        );
                        sendAcknowledgment(failureAck);
                    } else {
                        log.info("✅ OCR processing completed for document: {} with status: {}", 
                                document.id(), ocrResult.status());
                        
                        // Send OCR result to GenAI worker for summarization
                        sendOcrCompletionMessage(ocrResult);
                        
                        // Send acknowledgment (for backward compatibility)
                        OcrAcknowledgment ack = new OcrAcknowledgment(
                                document.id(),
                                document.title(),
                                ocrResult.status(),
                                ocrResult.getSummary(),
                                Instant.now()
                        );
                        sendAcknowledgment(ack);
                    }
                });
    }
    
    /**
     * Sends OCR completion message to GenAI worker for summarization.
     */
    private void sendOcrCompletionMessage(OcrResultDto ocrResult) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.OCR_COMPLETED_ROUTING_KEY,
                    ocrResult
            );
            log.info("📤 Sent OCR completion message to GenAI worker for document: {} ({} characters)",
                    ocrResult.documentId(), ocrResult.totalCharacters());
        } catch (Exception e) {
            log.error("❌ Failed to send OCR completion message for document: {}", ocrResult.documentId(), e);
        }
    }
    
    /**
     * Helper method to send acknowledgment messages.
     */
    private void sendAcknowledgment(OcrAcknowledgment acknowledgment) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    "document.created.ack",
                    acknowledgment
            );
            log.info("📤 Sent acknowledgment to queue for document: {}", acknowledgment.documentId());
        } catch (Exception e) {
            log.error("❌ Failed to send acknowledgment for document: {}", acknowledgment.documentId(), e);
        }
    }
}
