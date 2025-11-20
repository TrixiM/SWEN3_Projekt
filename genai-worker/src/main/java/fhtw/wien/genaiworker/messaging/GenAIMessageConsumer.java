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


    @RabbitListener(queues = RabbitMQConfig.OCR_COMPLETED_QUEUE)
    public void handleOcrCompleted(OcrResultDto message) {
        log.info("📨 Received OCR result for document: {} ('{})", message.documentId(), message.documentTitle());
        
        // Check if already processed
        if (idempotencyService.isAlreadyProcessed(message.messageId())) {
            log.info("⏭️ Skipping duplicate message: {}", message.messageId());
            return;
        }

        try {
            // Process summarization synchronously
            SummaryResultMessage result = summarizationService.processSummarization(message);
            
            // Mark as processed only if successful
            if (result.isSuccess()) {
                idempotencyService.markAsProcessed(message.messageId());
            }
            
            sendSummaryResult(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to process message: {}", message.documentId(), e);
            // Don't mark as processed on failure - allow retry
            sendSummaryResult(SummaryResultMessage.failure(
                    message.documentId(),
                    message.documentTitle(),
                    "Processing error: " + e.getMessage(),
                    0L
            ));
        }
    }


    private void sendSummaryResult(SummaryResultMessage result) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.SUMMARY_RESULT_ROUTING_KEY,
                    result
            );
            log.info("📤 Sent {} result for document: {}", result.status(), result.documentId());
        } catch (Exception e) {
            log.error("❌ Failed to send result: {}", result.documentId(), e);
        }
    }
}
