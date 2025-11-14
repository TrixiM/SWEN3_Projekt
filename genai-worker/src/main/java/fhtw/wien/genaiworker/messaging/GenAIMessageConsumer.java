package fhtw.wien.genaiworker.messaging;

import fhtw.wien.genaiworker.config.RabbitMQConfig;
import fhtw.wien.genaiworker.dto.OcrResultDto;
import fhtw.wien.genaiworker.dto.SummaryResultMessage;
import fhtw.wien.genaiworker.service.IdempotencyService;
import fhtw.wien.genaiworker.service.SummarizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for GenAI Worker.
 * Listens for OCR completion messages and triggers document summarization.
 */
@Component
public class GenAIMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenAIMessageConsumer.class);

    private final SummarizationService summarizationService;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;

    public GenAIMessageConsumer(SummarizationService summarizationService, 
                                RabbitTemplate rabbitTemplate,
                                IdempotencyService idempotencyService) {
        this.summarizationService = summarizationService;
        this.rabbitTemplate = rabbitTemplate;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Listens for OCR completion messages and triggers summarization.
     *
     * @param message the OCR completion message
     */
    @RabbitListener(queues = RabbitMQConfig.OCR_COMPLETED_QUEUE)
    public void handleOcrCompleted(OcrResultDto message) {
        log.info("📨 GENAI WORKER RECEIVED: OCR completed for document: {} ('{}')",
                message.documentId(), message.documentTitle());
        
        // Idempotency check
        String messageId = "genai-ocr-" + message.documentId();
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("⏭️ Skipping duplicate OCR completion for document: {}", message.documentId());
            return;
        }

        log.info("📋 OCR Details:");
        log.info("   - Pages: {}", message.totalPages());
        log.info("   - Characters: {}", message.totalCharacters());
        log.info("   - Confidence: {}%", message.overallConfidence());
        log.info("   - Status: {}", message.status());

        try {
            // Process summarization asynchronously
            log.info("🔄 Triggering summarization process...");

            summarizationService.processSummarization(message)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("❌ Summarization failed for document: {}", 
                                    message.documentId(), throwable);

                            // Send failure message
                            SummaryResultMessage failureMessage = SummaryResultMessage.failure(
                                    message.documentId(),
                                    message.documentTitle(),
                                    "Summarization error: " + throwable.getMessage(),
                                    0L
                            );
                            sendSummaryResult(failureMessage);

                        } else {
                            log.info("✅ Summarization completed for document: {} - Status: {}", 
                                    message.documentId(), result.status());

                            // Send result to backend
                            sendSummaryResult(result);
                        }
                    });

        } catch (Exception e) {
            log.error("❌ Failed to process OCR completion message for document: {}", 
                    message.documentId(), e);

            // Send failure message
            SummaryResultMessage failureMessage = SummaryResultMessage.failure(
                    message.documentId(),
                    message.documentTitle(),
                    "Processing error: " + e.getMessage(),
                    0L
            );
            sendSummaryResult(failureMessage);
        }
    }

    /**
     * Sends summary result to RabbitMQ for backend consumption.
     *
     * @param result the summary result message
     */
    private void sendSummaryResult(SummaryResultMessage result) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.SUMMARY_RESULT_ROUTING_KEY,
                    result
            );

            if (result.isSuccess()) {
                log.info("📤 Sent summary result to backend for document: {} (length: {} characters)",
                        result.documentId(), result.summary().length());
            } else {
                log.warn("📤 Sent failure result to backend for document: {} - Error: {}",
                        result.documentId(), result.errorMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to send summary result for document: {}", result.documentId(), e);
        }
    }
}
