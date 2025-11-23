package fhtw.wien.messaging;

import static fhtw.wien.config.MessagingConstants.*;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.exception.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDocumentCreated(DocumentResponse document) {
        log.info("Publishing document created event for document ID: {}", document.id());
        try {
            rabbitTemplate.convertAndSend(
                    DOCUMENT_EXCHANGE,
                    DOCUMENT_CREATED_ROUTING_KEY,
                    document
            );
            log.debug("Successfully published document created event for ID: {}", document.id());
        } catch (Exception e) {
            log.error("Failed to publish document created event for ID: {}", document.id(), e);
            throw new MessagingException("Failed to publish document created event", e);
        }
    }
}
