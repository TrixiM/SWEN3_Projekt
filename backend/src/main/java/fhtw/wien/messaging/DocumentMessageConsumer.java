package fhtw.wien.messaging;

import fhtw.wien.config.RabbitMQConfig;
import fhtw.wien.dto.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void handleDocumentCreated(DocumentResponse document) {
        log.info("Received document created event: {}", document);
        // Process document created event (e.g., trigger OCR, indexing, etc.)
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_DELETED_QUEUE)
    public void handleDocumentDeleted(String documentId) {
        log.info("Received document deleted event for ID: {}", documentId);
        // Process document deleted event (e.g., cleanup, remove from index, etc.)
    }
}
