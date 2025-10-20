package fhtw.wien.messaging;

import static fhtw.wien.config.MessagingConstants.*;
import fhtw.wien.exception.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentMessageConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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
}
