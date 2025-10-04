package fhtw.wien.messaging;

import fhtw.wien.config.RabbitMQConfig;
import fhtw.wien.dto.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDocumentCreated(DocumentResponse document) {
        log.info("Publishing document created event for document ID: {}", document.id());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_EXCHANGE,
                RabbitMQConfig.DOCUMENT_CREATED_ROUTING_KEY,
                document
        );
    }

    public void publishDocumentDeleted(UUID documentId) {
        log.info("Publishing document deleted event for document ID: {}", documentId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_EXCHANGE,
                RabbitMQConfig.DOCUMENT_DELETED_ROUTING_KEY,
                documentId.toString()
        );
    }
}
