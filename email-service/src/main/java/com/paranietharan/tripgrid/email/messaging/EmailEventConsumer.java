package com.paranietharan.tripgrid.email.messaging;

import com.paranietharan.tripgrid.email.dto.EmailVerificationRequestedEvent;
import com.paranietharan.tripgrid.email.dto.UserRegisteredEvent;
import com.paranietharan.tripgrid.email.service.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailEventConsumer.class);

    private final EmailSenderService emailSenderService;

    public EmailEventConsumer(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @RabbitListener(queues = "${tripgrid.rabbitmq.queues.user-registered:email.user-registered}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user id: {}, email: {}", event.getUserId(), event.getEmail());
        emailSenderService.sendVerificationEmail(
                event.getEmail(),
                event.getFirstName(),
                event.getVerificationCode()
        );
    }

    @RabbitListener(queues = "${tripgrid.rabbitmq.queues.verification:email.verification}")
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Received EmailVerificationRequestedEvent for email: {}", event.getEmail());
        emailSenderService.sendVerificationEmail(
                event.getEmail(),
                event.getFirstName(),
                event.getVerificationCode()
        );
    }
}
