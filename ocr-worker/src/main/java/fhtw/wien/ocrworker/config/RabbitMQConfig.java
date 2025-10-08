package fhtw.wien.ocrworker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_CREATED_QUEUE = "document.created.queue";
    public static final String DOCUMENT_CREATED_ACK_QUEUE = "document.created.ack.queue";
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE);
    }

    @Bean
    public Queue documentCreatedQueue() {
        return new Queue(DOCUMENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue documentCreatedAckQueue() {
        return new Queue(DOCUMENT_CREATED_ACK_QUEUE, true);
    }

    @Bean
    public Binding documentCreatedBinding(Queue documentCreatedQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentCreatedQueue)
                .to(documentExchange)
                .with(DOCUMENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding documentCreatedAckBinding(Queue documentCreatedAckQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentCreatedAckQueue)
                .to(documentExchange)
                .with("document.created.ack");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
