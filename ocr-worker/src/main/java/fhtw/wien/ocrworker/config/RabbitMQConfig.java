package fhtw.wien.ocrworker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_CREATED_QUEUE = "document.created.queue";
    public static final String DOCUMENT_CREATED_ACK_QUEUE = "document.created.ack.queue";
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String OCR_COMPLETED_ROUTING_KEY = "ocr.completed";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }
    
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE + ".dlx", true, false);
    }

    @Bean
    public Queue documentCreatedQueue() {
        return createQueueWithDLQ(DOCUMENT_CREATED_QUEUE);
    }
    
    @Bean
    public Queue documentCreatedDLQ() {
        return new Queue(DOCUMENT_CREATED_QUEUE + ".dlq", true);
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
        rabbitTemplate.setMandatory(true);
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
    
    private Queue createQueueWithDLQ(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DOCUMENT_EXCHANGE + ".dlx");
        args.put("x-dead-letter-routing-key", queueName + ".dlq");
        // Note: x-message-ttl removed to avoid conflicts with existing queues
        return new Queue(queueName, true, false, false, args);
    }
    
    @Bean
    public Binding documentCreatedDLQBinding(Queue documentCreatedDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(documentCreatedDLQ)
                .to(deadLetterExchange)
                .with(DOCUMENT_CREATED_QUEUE + ".dlq");
    }
}
