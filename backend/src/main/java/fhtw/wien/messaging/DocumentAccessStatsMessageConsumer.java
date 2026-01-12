package fhtw.wien.messaging;

import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentAccessStat;
import fhtw.wien.dto.DocumentAccessStatDTO;
import fhtw.wien.exception.MessagingException;
import fhtw.wien.repo.DocumentAccessStatRepo;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.transaction.annotation.Transactional;

import static fhtw.wien.config.MessagingConstants.DOCUMENT_ACCESS_STATS_QUEUE;

public class DocumentAccessStatsMessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(DocumentAccessStatsMessageConsumer.class);

    private final DocumentAccessStatRepo documentAccessStatRepo;
    private final IdempotencyService idempotencyService;
    private final DocumentRepo documentRepo;

    public DocumentAccessStatsMessageConsumer(DocumentAccessStatRepo documentAccessStatRepo, IdempotencyService idempotencyService, DocumentRepo documentRepo) {
        this.documentAccessStatRepo = documentAccessStatRepo;
        this.idempotencyService = idempotencyService;
        this.documentRepo = documentRepo;
    }

    @RabbitListener (queues=DOCUMENT_ACCESS_STATS_QUEUE)
    @Transactional
    public void handleAccessStats(DocumentAccessStatDTO dto){
        log.info("Access stats received for document {}", dto.documentId());

        if (!idempotencyService.tryMarkAsProcessed(dto.messageId())) {
            log.info("Skipping duplicate access stat message {}", dto.messageId());
            return;
        }

        Document document = documentRepo.findById(dto.documentId()).orElseThrow(() -> {
            log.warn("Document not found for document access stats import: {}", dto.documentId());
            return new MessagingException("Document not found: " + dto.documentId());
        });

        DocumentAccessStat stat = new DocumentAccessStat(
                document,
                dto.accessCount(),
                dto.lastAccessedAt()
        );

        documentAccessStatRepo.save(stat);

        log.info("Access stats persisted for document {}", dto.documentId());
    }
}
