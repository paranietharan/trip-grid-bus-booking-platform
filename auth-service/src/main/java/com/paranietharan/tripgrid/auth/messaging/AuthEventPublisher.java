package com.paranietharan.tripgrid.auth.messaging;

import com.paranietharan.tripgrid.auth.messaging.event.EmailVerificationRequestedEvent;
import com.paranietharan.tripgrid.auth.messaging.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String userRegisteredRoutingKey;
    private final String verificationRequestedRoutingKey;

    public AuthEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${tripgrid.rabbitmq.exchange:tripgrid.events}") String exchangeName,
            @Value("${tripgrid.rabbitmq.routing-keys.user-registered:user.registered}") String userRegisteredRoutingKey,
            @Value("${tripgrid.rabbitmq.routing-keys.verification-requested:email.verification.requested}") String verificationRequestedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.userRegisteredRoutingKey = userRegisteredRoutingKey;
        this.verificationRequestedRoutingKey = verificationRequestedRoutingKey;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for user id: {}, email: {}", event.getUserId(), event.getEmail());
        rabbitTemplate.convertAndSend(exchangeName, userRegisteredRoutingKey, event);
    }

    public void publishVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Publishing EmailVerificationRequestedEvent for email: {}", event.getEmail());
        rabbitTemplate.convertAndSend(exchangeName, verificationRequestedRoutingKey, event);
    }
}
