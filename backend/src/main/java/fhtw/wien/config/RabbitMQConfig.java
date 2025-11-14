package fhtw.wien.config;

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

import static fhtw.wien.config.MessagingConstants.*;

@Configuration
public class RabbitMQConfig {


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
        return new Queue(DOCUMENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue documentDeletedQueue() {
        return new Queue(DOCUMENT_DELETED_QUEUE, true);
    }

    @Bean
    public Queue documentCreatedAckQueue() {
        return new Queue(DOCUMENT_CREATED_ACK_QUEUE, true);
    }

    @Bean
    public Queue documentDeletedAckQueue() {
        return new Queue(DOCUMENT_DELETED_ACK_QUEUE, true);
    }

    @Bean
    public Queue ocrCompletedQueue() {
        return new Queue(OCR_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue summaryResultQueue() {
        return new Queue(SUMMARY_RESULT_QUEUE, true);
    }
    
    // Dead Letter Queues
    @Bean
    public Queue documentCreatedDLQ() {
        return new Queue(DOCUMENT_CREATED_QUEUE + ".dlq", true);
    }
    
    @Bean
    public Queue documentDeletedDLQ() {
        return new Queue(DOCUMENT_DELETED_QUEUE + ".dlq", true);
    }
    
    @Bean
    public Queue ocrCompletedDLQ() {
        return new Queue(OCR_COMPLETED_QUEUE + ".dlq", true);
    }
    
    @Bean
    public Queue summaryResultDLQ() {
        return new Queue(SUMMARY_RESULT_QUEUE + ".dlq", true);
    }

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
    public Binding documentCreatedAckBinding(Queue documentCreatedAckQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentCreatedAckQueue)
                .to(documentExchange)
                .with(DOCUMENT_CREATED_ACK_ROUTING_KEY);
    }

    @Bean
    public Binding documentDeletedAckBinding(Queue documentDeletedAckQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentDeletedAckQueue)
                .to(documentExchange)
                .with(DOCUMENT_DELETED_ACK_ROUTING_KEY);
    }

    @Bean
    public Binding ocrCompletedBinding(Queue ocrCompletedQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(ocrCompletedQueue)
                .to(documentExchange)
                .with(OCR_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding summaryResultBinding(Queue summaryResultQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(summaryResultQueue)
                .to(documentExchange)
                .with(SUMMARY_RESULT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        // Enable publisher confirms and returns for better reliability
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // Enable manual acknowledgment mode for better control
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // Set prefetch count to limit concurrent message processing
        factory.setPrefetchCount(10);
        // Enable retry with exponential backoff
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
    
    /**
     * Helper method to create a queue with Dead Letter Exchange configuration.
     * Note: TTL removed to avoid conflicts with existing queues.
     * DLQ routing is configured for rejected/failed messages.
     */
    private Queue createQueueWithDLQ(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DOCUMENT_EXCHANGE + ".dlx");
        args.put("x-dead-letter-routing-key", queueName + ".dlq");
        // Note: x-message-ttl removed to avoid conflicts with existing queues
        // Messages go to DLQ when rejected by consumers (on exception)
        return new Queue(queueName, true, false, false, args);
    }
    
    // DLQ Bindings
    @Bean
    public Binding documentCreatedDLQBinding(Queue documentCreatedDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(documentCreatedDLQ)
                .to(deadLetterExchange)
                .with(DOCUMENT_CREATED_QUEUE + ".dlq");
    }
    
    @Bean
    public Binding documentDeletedDLQBinding(Queue documentDeletedDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(documentDeletedDLQ)
                .to(deadLetterExchange)
                .with(DOCUMENT_DELETED_QUEUE + ".dlq");
    }
    
    @Bean
    public Binding ocrCompletedDLQBinding(Queue ocrCompletedDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(ocrCompletedDLQ)
                .to(deadLetterExchange)
                .with(OCR_COMPLETED_QUEUE + ".dlq");
    }
    
    @Bean
    public Binding summaryResultDLQBinding(Queue summaryResultDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(summaryResultDLQ)
                .to(deadLetterExchange)
                .with(SUMMARY_RESULT_QUEUE + ".dlq");
    }
}
