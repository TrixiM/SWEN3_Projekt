package fhtw.wien.ocrworker.messaging;

import fhtw.wien.ocrworker.config.RabbitMQConfig;
import fhtw.wien.ocrworker.dto.Document;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import fhtw.wien.ocrworker.elasticsearch.ElasticsearchService;
import fhtw.wien.ocrworker.service.IdempotencyService;
import fhtw.wien.ocrworker.service.UnifiedOcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OcrMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final UnifiedOcrService ocrProcessingService;
    private final IdempotencyService idempotencyService;
    private final ElasticsearchService elasticsearchService;

    public OcrMessageConsumer(RabbitTemplate rabbitTemplate,
                              UnifiedOcrService ocrProcessingService,
                              IdempotencyService idempotencyService,
                              ElasticsearchService elasticsearchService) {
        this.rabbitTemplate = rabbitTemplate;
        this.ocrProcessingService = ocrProcessingService;
        this.idempotencyService = idempotencyService;
        this.elasticsearchService = elasticsearchService;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void handleDocumentCreated(Document document) {
        log.info("OCR started: id={}, file='{}'", document.id(), document.originalFilename());

        String messageId = "ocr-doc-" + document.id();
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("Skipping duplicate message for {}", document.id());
            return;
        }

        OcrResultDto ocrResult = ocrProcessingService.processDocument(document);
        log.info("OCR done: id={}, chars={}, status={}", document.id(), ocrResult.totalCharacters(), ocrResult.status());

        if (ocrResult.isSuccess() && ocrResult.extractedText() != null && !ocrResult.extractedText().isEmpty()) {
            try {
                elasticsearchService.indexDocument(ocrResult);
            } catch (Exception e) {
                log.error("Elasticsearch indexing failed for {}", document.id(), e);
            }
        }

        sendOcrCompletionMessage(ocrResult);
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_DELETED_QUEUE)
    public void handleDocumentDeleted(UUID id) {
        log.info("Received document deletion event: id={}", id);

        String messageId = "delete-doc-" + id;
        if (!idempotencyService.tryMarkAsProcessed(messageId)) {
            log.info("Skipping duplicate delete message for {}", id);
            return;
        }

        try {
            elasticsearchService.deleteDocument(id);
            log.info("Deleted document {} from Elasticsearch", id);
        } catch (Exception e) {
            log.error("Failed to delete document {} from Elasticsearch", id, e);
        }
    }


    private void sendOcrCompletionMessage(OcrResultDto ocrResult) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.OCR_COMPLETED_ROUTING_KEY,
                    ocrResult
            );
            log.info("Sent OCR result to GenAI: id={}, status={}", ocrResult.documentId(), ocrResult.status());
        } catch (Exception e) {
            log.error("Failed to send OCR result for {}", ocrResult.documentId(), e);
        }
    }
}
