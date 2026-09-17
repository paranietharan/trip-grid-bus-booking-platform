package com.paranietharan.tripgrid.email.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class UserRegisteredEvent implements Serializable {

    private UUID userId;
    private String email;
    private String firstName;
    private String verificationCode;
    private Instant timestamp;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(UUID userId, String email, String firstName, String verificationCode, Instant timestamp) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.verificationCode = verificationCode;
        this.timestamp = timestamp;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
