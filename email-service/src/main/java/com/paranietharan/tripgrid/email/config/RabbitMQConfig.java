package com.paranietharan.tripgrid.email.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${tripgrid.rabbitmq.exchange:tripgrid.events}")
    private String exchangeName;

    @Value("${tripgrid.rabbitmq.dlx:tripgrid.events.dlx}")
    private String dlxExchangeName;

    @Value("${tripgrid.rabbitmq.queues.user-registered:email.user-registered}")
    private String userRegisteredQueueName;

    @Value("${tripgrid.rabbitmq.queues.verification:email.verification}")
    private String verificationQueueName;

    @Value("${tripgrid.rabbitmq.queues.dlq:email.dlq}")
    private String dlqName;

    @Value("${tripgrid.rabbitmq.routing-keys.user-registered:user.registered}")
    private String userRegisteredRoutingKey;

    @Value("${tripgrid.rabbitmq.routing-keys.verification-requested:email.verification.requested}")
    private String verificationRequestedRoutingKey;

    @Value("${tripgrid.rabbitmq.routing-keys.dlq:email.dead-letter}")
    private String dlqRoutingKey;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchangeName, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public Queue userRegisteredQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchangeName);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return new Queue(userRegisteredQueueName, true, false, false, args);
    }

    @Bean
    public Queue verificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchangeName);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return new Queue(verificationQueueName, true, false, false, args);
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(eventsExchange).with(userRegisteredRoutingKey);
    }

    @Bean
    public Binding verificationBinding(Queue verificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(verificationQueue).to(eventsExchange).with(verificationRequestedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
