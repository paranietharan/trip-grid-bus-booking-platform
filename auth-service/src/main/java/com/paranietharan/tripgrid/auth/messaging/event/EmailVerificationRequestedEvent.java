package com.paranietharan.tripgrid.auth.messaging.event;

import java.io.Serializable;
import java.time.Instant;

public class EmailVerificationRequestedEvent implements Serializable {

    private String email;
    private String firstName;
    private String verificationCode;
    private Instant timestamp;

    public EmailVerificationRequestedEvent() {
        this.timestamp = Instant.now();
    }

    public EmailVerificationRequestedEvent(String email, String firstName, String verificationCode) {
        this.email = email;
        this.firstName = firstName;
        this.verificationCode = verificationCode;
        this.timestamp = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
