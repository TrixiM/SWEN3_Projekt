package fhtw.wien.ocrworker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
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
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String OCR_COMPLETED_ROUTING_KEY = "ocr.completed";
    public static final String DOCUMENT_DELETED_QUEUE = "document.deleted.queue";
    public static final String DOCUMENT_DELETED_ROUTING_KEY = "document.deleted";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue documentCreatedQueue() {
        return new Queue(DOCUMENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue documentDeletedQueue() {return new Queue(DOCUMENT_DELETED_QUEUE, true);}

    @Bean
    public Binding documentCreatedBinding(Queue documentCreatedQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentCreatedQueue)
                .to(documentExchange)
                .with(DOCUMENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding documentDeletedBinding(Queue documentDeletedQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentDeletedQueue)
                .to(documentExchange)
                .with(DOCUMENT_DELETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setMandatory(true); //message must be routed to a queue, otherwise return to producer
        return rabbitTemplate;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
