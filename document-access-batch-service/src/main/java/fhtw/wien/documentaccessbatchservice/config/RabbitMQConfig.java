package fhtw.wien.documentaccessbatchservice.config;

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

    public static final String DOCUMENT_ACCESS_STATS_ROUTING_KEY = "document.access.stats";
    public static final String DOCUMENT_ACCESS_STATS_QUEUE = "document.access.stats.queue";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue documentAccessStatsQueue() {
        return new Queue(DOCUMENT_ACCESS_STATS_QUEUE, true);
    }

    @Bean
    public Binding ocrCompletedBinding(Queue documentAccessStatsQueue, DirectExchange documentExchange) {
        return BindingBuilder.bind(documentAccessStatsQueue)
                .to(documentExchange)
                .with(DOCUMENT_ACCESS_STATS_ROUTING_KEY);
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
}
