package fhtw.wien.messaging;

import static fhtw.wien.config.MessagingConstants.*;
import fhtw.wien.domain.Document;
import fhtw.wien.dto.SummaryResultDto;
import fhtw.wien.exception.MessagingException;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final DocumentRepo documentRepo;
    private final IdempotencyService idempotencyService;

    public DocumentMessageConsumer(RabbitTemplate rabbitTemplate, DocumentRepo documentRepo, 
                                    IdempotencyService idempotencyService) {
        this.rabbitTemplate = rabbitTemplate;
        this.documentRepo = documentRepo;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(queues = DOCUMENT_DELETED_QUEUE)
    public void handleDocumentDeleted(String documentId) {
        log.info("✅ CONSUMER RECEIVED: Document deleted - ID: {}", documentId);

        try {
            // Send acknowledgment message back to a response queue
            String ackMessage = String.format(
                    "✅ Acknowledged: Document ID: %s deletion received and processed",
                    documentId
            );

            rabbitTemplate.convertAndSend(
                    DOCUMENT_EXCHANGE,
                    DOCUMENT_DELETED_ACK_ROUTING_KEY,
                    ackMessage
            );

            log.info("📤 Sent acknowledgment to queue for document ID: {}", documentId);
            // Process document deleted event (e.g., cleanup, remove from index, etc.)
        } catch (Exception e) {
            log.error("Failed to process document deleted event for ID: {}", documentId, e);
            throw new MessagingException("Failed to process document deleted event", e);
        }
    }


    @RabbitListener(queues = SUMMARY_RESULT_QUEUE)
    @Transactional
    public void handleSummaryResult(SummaryResultDto summaryResult) {
        log.info("📨 BACKEND RECEIVED: Summary result for document ID: {}", summaryResult.documentId());
        
        // Idempotency check using unique messageId
        if (!idempotencyService.tryMarkAsProcessed(summaryResult.messageId())) {
            log.info("⏭️ Skipping duplicate summary result message: {}", summaryResult.messageId());
            return;
        }

        try {
            if (summaryResult.isSuccess()) {
                // Find document and update summary
                Document document = documentRepo.findById(summaryResult.documentId())
                        .orElseThrow(() -> {
                            log.warn("Document not found for summary update: {}", summaryResult.documentId());
                            return new MessagingException("Document not found: " + summaryResult.documentId());
                        });

                document.setSummary(summaryResult.summary());
                documentRepo.save(document);

                log.info("✅ Summary saved for document: {} (length: {} characters)",
                        summaryResult.documentId(), summaryResult.summary().length());

            } else {
                log.warn("⚠️ Summary generation failed for document: {} - {}",
                        summaryResult.documentId(), summaryResult.errorMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to process summary result for document: {}", summaryResult.documentId(), e);
            throw new MessagingException("Failed to process summary result", e);
        }
    }
}
