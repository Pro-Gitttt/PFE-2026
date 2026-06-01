package com.example.notificationservice.SERVICES;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    // Optional — may be null if spring.mail.username is not set
    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean enabled;

    @Value("${notification.email.from:noreply@devsecops.local}")
    private String from;

    public EmailService(
            @org.springframework.lang.Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        if (!enabled || mailSender == null) {
            log.info("Email disabled or not configured — skipping send to {}", to);
            // Throw so caller marks notification as FAILED (not SENT)
            throw new RuntimeException("Email not configured");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Email failed to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage());
        }
    }
}
