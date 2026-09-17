package com.paranietharan.tripgrid.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;

    public EmailSenderService(
            JavaMailSender mailSender,
            @Value("${tripgrid.mail.from:no-reply@tripgrid.com}") String fromEmail,
            @Value("${tripgrid.mail.from-name:TripGrid}") String fromName) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public void sendVerificationEmail(String toEmail, String firstName, String verificationCode) {
        log.info("Preparing verification email for recipient: {}", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("TripGrid - Verify Your Email Address");

            String htmlContent = buildVerificationEmailHtml(firstName, verificationCode);
            String textContent = String.format(
                    "Hello %s,\n\nWelcome to TripGrid! Your 6-digit email verification code is: %s\n\nThis code will expire in 10 minutes.\n\nIf you did not create a TripGrid account, please ignore this email.\n\nBest regards,\nTripGrid Team",
                    firstName, verificationCode
            );

            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Verification email successfully dispatched to {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send verification email to {}", toEmail, e);
            throw new RuntimeException("Email delivery failed for recipient: " + toEmail, e);
        }
    }

    private String buildVerificationEmailHtml(String firstName, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>TripGrid Email Verification</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                    .header { background-color: #1a56db; padding: 30px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                    .body { padding: 40px 30px; color: #374151; line-height: 1.6; }
                    .body h2 { font-size: 20px; margin-top: 0; color: #111827; }
                    .code-box { background-color: #f3f4f6; border: 2px dashed #d1d5db; border-radius: 6px; padding: 20px; text-align: center; margin: 30px 0; }
                    .code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #1a56db; font-family: monospace; }
                    .note { font-size: 13px; color: #6b7280; margin-top: 20px; }
                    .footer { background-color: #f9fafb; padding: 20px 30px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h1>TripGrid</h1>
                    </div>
                    <div class="body">
                      <h2>Verify Your Email</h2>
                      <p>Hello %s,</p>
                      <p>Thank you for signing up with TripGrid. To complete your registration and secure your account, please enter the following 6-digit verification code:</p>
                      <div class="code-box">
                        <div class="code">%s</div>
                      </div>
                      <p class="note"><strong>Note:</strong> This verification code expires in 10 minutes and can only be used once.</p>
                      <p>If you did not create an account on TripGrid, please disregard this email.</p>
                      <p>Safe travels,<br><strong>The TripGrid Team</strong></p>
                    </div>
                    <div class="footer">
                      &copy; 2026 TripGrid Bus Booking Platform. All rights reserved.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                org.springframework.web.util.HtmlUtils.htmlEscape(firstName == null ? "" : firstName),
                org.springframework.web.util.HtmlUtils.htmlEscape(code == null ? "" : code)
        );
    }
}
