package fhtw.wien.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static fhtw.wien.config.MessagingConstants.*;

@Configuration
public class RabbitMQConfig {


    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue summaryResultQueue() {
        return new Queue(SUMMARY_RESULT_QUEUE, true);
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
}
