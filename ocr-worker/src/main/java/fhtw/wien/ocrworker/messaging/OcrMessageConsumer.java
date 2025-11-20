package fhtw.wien.ocrworker.messaging;

import fhtw.wien.ocrworker.config.RabbitMQConfig;
import fhtw.wien.ocrworker.dto.DocumentResponse;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import fhtw.wien.ocrworker.elasticsearch.ElasticsearchService;
import fhtw.wien.ocrworker.service.IdempotencyService;
import fhtw.wien.ocrworker.service.OcrProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageConsumer.class);

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
        log.info("📄 OCR started: id={}, file='{}'", document.id(), document.originalFilename());
        
        // Idempotency check
        String messageId = "ocr-doc-" + document.id();
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("⏭️ Skipping duplicate: {}", document.id());
            return;
        }
        
        OcrResultDto ocrResult = ocrProcessingService.processDocument(document);
        
        log.info("✅ OCR done: id={}, chars={}", document.id(), ocrResult.totalCharacters());
        
        // Index document in Elasticsearch if OCR was successful
        if (ocrResult.isSuccess() && ocrResult.extractedText() != null && !ocrResult.extractedText().isEmpty()) {
            try {
                elasticsearchService.indexDocument(ocrResult);
            } catch (Exception e) {
                log.error("❌ Elasticsearch indexing failed: {}", document.id(), e);
            }
        }
        
        // Send OCR result to GenAI worker for summarization
        sendOcrCompletionMessage(ocrResult);
    }
    

    private void sendOcrCompletionMessage(OcrResultDto ocrResult) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.OCR_COMPLETED_ROUTING_KEY,
                    ocrResult
            );
            log.info("📤 Sent OCR result to GenAI: id={}, status={}", ocrResult.documentId(), ocrResult.status());
        } catch (Exception e) {
            log.error("❌ Failed to send OCR result: {}", ocrResult.documentId(), e);
        }
    }
}
