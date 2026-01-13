package fhtw.wien.documentaccessbatchservice.messaging;
import fhtw.wien.documentaccessbatchservice.dto.DocumentAccessStatDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import fhtw.wien.documentaccessbatchservice.config.RabbitMQConfig.*;

import static fhtw.wien.documentaccessbatchservice.config.RabbitMQConfig.DOCUMENT_ACCESS_STATS_ROUTING_KEY;
import static fhtw.wien.documentaccessbatchservice.config.RabbitMQConfig.DOCUMENT_EXCHANGE;

@Component
public class DocumentAccessStatProducer {

    private final RabbitTemplate rabbitTemplate;

    public DocumentAccessStatProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(DocumentAccessStatDto message) {
        rabbitTemplate.convertAndSend(
                DOCUMENT_EXCHANGE,
                DOCUMENT_ACCESS_STATS_ROUTING_KEY,
                message
        );
    }
}