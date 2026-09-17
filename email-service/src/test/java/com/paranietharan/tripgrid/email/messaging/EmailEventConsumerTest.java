package com.paranietharan.tripgrid.email.messaging;

import com.paranietharan.tripgrid.email.dto.EmailVerificationRequestedEvent;
import com.paranietharan.tripgrid.email.dto.UserRegisteredEvent;
import com.paranietharan.tripgrid.email.service.EmailSenderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailEventConsumerTest {

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailEventConsumer emailEventConsumer;

    @Test
    @DisplayName("Should consume UserRegisteredEvent and call EmailSenderService")
    void shouldHandleUserRegistered() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(),
                "test@example.com",
                "Alice",
                "654321",
                Instant.now()
        );

        emailEventConsumer.handleUserRegistered(event);

        verify(emailSenderService).sendVerificationEmail("test@example.com", "Alice", "654321");
    }

    @Test
    @DisplayName("Should consume EmailVerificationRequestedEvent and call EmailSenderService")
    void shouldHandleEmailVerificationRequested() {
        EmailVerificationRequestedEvent event = new EmailVerificationRequestedEvent(
                "test@example.com",
                "Alice",
                "112233",
                Instant.now()
        );

        emailEventConsumer.handleEmailVerificationRequested(event);

        verify(emailSenderService).sendVerificationEmail("test@example.com", "Alice", "112233");
    }
}
