package fhtw.wien.messaging;

import static fhtw.wien.config.MessagingConstants.*;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.MessagingException;
import fhtw.wien.service.DocumentMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentMessageProducer implements DocumentMessageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishDocumentCreated(Document document) {
        log.info("Publishing document created event for document ID: {}", document.getId());
        try {
            rabbitTemplate.convertAndSend(
                    DOCUMENT_EXCHANGE,
                    DOCUMENT_CREATED_ROUTING_KEY,
                    document
            );
            log.debug("Successfully published document created event for ID: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to publish document created event for ID: {}", document.getId(), e);
            throw new MessagingException("Failed to publish document created event", e);
        }
    }
    @Override
    public void deleteDocument(UUID id) {
        log.info("Deleting elasticsearch index of document with ID: {}", id);
        try{
            rabbitTemplate.convertAndSend(
                    DOCUMENT_EXCHANGE,
                    DOCUMENT_DELETED_ROUTING_KEY,
                    id
            );
            log.debug("Successfully deleted elasticsearch index of document with ID: {}", id);

        }catch (Exception e){
            log.error("Failed to delete elasticsearch index of document with ID: {}", id, e);
            throw new MessagingException("Failed to delete elasticsearch index", e);
        }
    }
}
