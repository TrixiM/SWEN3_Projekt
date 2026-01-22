package fhtw.wien.messaging;

import static fhtw.wien.config.MessagingConstants.SUMMARY_RESULT_QUEUE;
import fhtw.wien.domain.Document;
import fhtw.wien.dto.SummaryResultDto;
import fhtw.wien.exception.MessagingException;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    private final DocumentRepo documentRepo;
    private final IdempotencyService idempotencyService;

    public DocumentMessageConsumer(DocumentRepo documentRepo,
                                   IdempotencyService idempotencyService) {
        this.documentRepo = documentRepo;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(queues = SUMMARY_RESULT_QUEUE)
    @Transactional
    public void handleSummaryResult(SummaryResultDto summaryResult) {
        log.info("Summary result received for document ID: {}", summaryResult.documentId());

        if (!idempotencyService.tryMarkAsProcessed(summaryResult.messageId())) {
            log.info("Skipping duplicate summary result message: {}", summaryResult.messageId());
            return;
        }

        try {
            if (summaryResult.isSuccess()) {
                Document document = documentRepo.findById(summaryResult.documentId())
                        .orElseThrow(() -> {
                            log.warn("Document not found for summary update: {}", summaryResult.documentId());
                            return new MessagingException("Document not found: " + summaryResult.documentId());
                        });

                document.setSummary(summaryResult.summary());
                documentRepo.save(document);

                log.info("Summary saved for document: {} (length: {} characters)",
                        summaryResult.documentId(), summaryResult.summary().length());

            } else {
                log.warn("Summary generation failed for document: {} - {}",
                        summaryResult.documentId(), summaryResult.errorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process summary result for document: {}", summaryResult.documentId(), e);
            throw new MessagingException("Failed to process summary result", e);
        }
    }
}
