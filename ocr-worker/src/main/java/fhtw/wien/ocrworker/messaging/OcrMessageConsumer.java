package fhtw.wien.ocrworker.messaging;

import fhtw.wien.ocrworker.config.RabbitMQConfig;
import fhtw.wien.ocrworker.dto.DocumentResponse;
import fhtw.wien.ocrworker.dto.OcrAcknowledgment;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import fhtw.wien.ocrworker.elasticsearch.ElasticsearchService;
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
    private static final String IDEMPOTENCY_PREFIX = "ocr-doc-";

    private final RabbitTemplate rabbitTemplate;
    private final OcrProcessingService ocrProcessingService;
    private final IdempotencyService idempotencyService;
    private final ElasticsearchService elasticsearchService;

    public OcrMessageConsumer(RabbitTemplate rabbitTemplate, OcrProcessingService ocrProcessingService,
                              IdempotencyService idempotencyService, ElasticsearchService elasticsearchService) {
        this.rabbitTemplate = rabbitTemplate;
        this.ocrProcessingService = ocrProcessingService;
        this.idempotencyService = idempotencyService;
        this.elasticsearchService = elasticsearchService;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void handleDocumentCreated(DocumentResponse document) {
        log.info("📄 OCR WORKER RECEIVED: Document created - ID: {}, Title: '{}'",
                document.id(), document.title());
        
        if (!checkIdempotency(document)) {
            return;
        }
        
        logDocumentDetails(document);
        
        log.info("🔄 Delegating OCR processing to service...");
        ocrProcessingService.processDocument(document)
                .whenComplete((ocrResult, throwable) -> 
                        handleOcrCompletion(document, ocrResult, throwable));
    }
    
    /**
     * Checks idempotency to prevent duplicate processing.
     */
    private boolean checkIdempotency(DocumentResponse document) {
        String messageId = IDEMPOTENCY_PREFIX + document.id();
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("⏭️ Skipping duplicate document processing: {}", document.id());
            return false;
        }
        return true;
    }
    
    /**
     * Logs document details for debugging.
     */
    private void logDocumentDetails(DocumentResponse document) {
        log.info("📋 Document Details:");
        log.info("   - Filename: {}", document.originalFilename());
        log.info("   - Content Type: {}", document.contentType());
        log.info("   - Size: {} bytes", document.sizeBytes());
        log.info("   - Status: {}", document.status());
    }
    
    /**
     * Handles OCR processing completion (success or failure).
     */
    private void handleOcrCompletion(DocumentResponse document, OcrResultDto ocrResult, Throwable throwable) {
        if (throwable != null) {
            handleOcrFailure(document, throwable);
        } else {
            handleOcrSuccess(document, ocrResult);
        }
    }
    
    /**
     * Handles OCR processing failure.
     */
    private void handleOcrFailure(DocumentResponse document, Throwable throwable) {
        log.error("❌ OCR processing failed for document: {}", document.id(), throwable);
        
        OcrAcknowledgment failureAck = new OcrAcknowledgment(
                document.id(),
                document.title(),
                "FAILED",
                "Processing failed: " + throwable.getMessage(),
                Instant.now()
        );
        sendAcknowledgment(failureAck);
    }
    
    /**
     * Handles OCR processing success.
     */
    private void handleOcrSuccess(DocumentResponse document, OcrResultDto ocrResult) {
        log.info("✅ OCR processing completed for document: {} with status: {}", 
                document.id(), ocrResult.status());
        
        if (shouldIndexDocument(ocrResult)) {
            indexDocumentInElasticsearch(ocrResult);
        }
        
        sendOcrCompletionMessage(ocrResult);
        sendSuccessAcknowledgment(document, ocrResult);
    }
    
    /**
     * Determines if document should be indexed in Elasticsearch.
     */
    private boolean shouldIndexDocument(OcrResultDto ocrResult) {
        return ocrResult.isSuccess() && 
               ocrResult.extractedText() != null && 
               !ocrResult.extractedText().isEmpty();
    }
    
    /**
     * Indexes document in Elasticsearch.
     */
    private void indexDocumentInElasticsearch(OcrResultDto ocrResult) {
        try {
            elasticsearchService.indexDocument(ocrResult);
            log.info("📇 Document {} successfully indexed in Elasticsearch", ocrResult.documentId());
        } catch (Exception e) {
            log.error("❌ Failed to index document {} in Elasticsearch: {}", 
                    ocrResult.documentId(), e.getMessage(), e);
        }
    }
    
    /**
     * Sends success acknowledgment.
     */
    private void sendSuccessAcknowledgment(DocumentResponse document, OcrResultDto ocrResult) {
        OcrAcknowledgment ack = new OcrAcknowledgment(
                document.id(),
                document.title(),
                ocrResult.status(),
                ocrResult.getSummary(),
                Instant.now()
        );
        sendAcknowledgment(ack);
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
     * Sends acknowledgment message to the queue.
     */
    private void sendAcknowledgment(OcrAcknowledgment acknowledgment) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    "document.created.ack", // TODO: Move to RabbitMQConfig constants
                    acknowledgment
            );
            log.info("📤 Sent acknowledgment to queue for document: {}", acknowledgment.documentId());
        } catch (Exception e) {
            log.error("❌ Failed to send acknowledgment for document: {}", acknowledgment.documentId(), e);
        }
    }
}
