package fhtw.wien.genaiworker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for GenAI Worker.
 * Defines queues, exchanges, and bindings for OCR completion and summary result messages.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    
    // Queues
    public static final String OCR_COMPLETED_QUEUE = "ocr.completed.queue";
    public static final String SUMMARY_RESULT_QUEUE = "summary.result.queue";
    
    // Routing Keys
    public static final String OCR_COMPLETED_ROUTING_KEY = "ocr.completed";
    public static final String SUMMARY_RESULT_ROUTING_KEY = "summary.result";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE);
    }

    @Bean
    public Queue ocrCompletedQueue() {
        return new Queue(OCR_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue summaryResultQueue() {
        return new Queue(SUMMARY_RESULT_QUEUE, true);
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
        return rabbitTemplate;
    }
}
