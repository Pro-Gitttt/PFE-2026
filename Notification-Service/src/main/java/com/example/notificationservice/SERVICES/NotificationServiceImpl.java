package com.example.notificationservice.SERVICES;

import com.example.notificationservice.CLIENT.UserClient;
import com.example.notificationservice.DTOs.NotificationResponse;
import com.example.notificationservice.DTOs.PipelineEventRequest;
import com.example.notificationservice.DTOs.SecurityAlertRequest;
import com.example.notificationservice.entities.*;
import com.example.notificationservice.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService           emailService;
    private final SlackService           slackService;
    private final UserClient             userClient;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.default-recipient:admin@devsecops.local}")
    private String defaultRecipient;

    @Override
    public NotificationResponse handlePipelineEvent(PipelineEventRequest req) {
        String subject = "[DevSecOps] Pipeline "
                + (req.getEventType() == EventType.PIPELINE_SUCCESS ? "SUCCESS ✓" : "FAILED ✗")
                + " — " + req.getProjectName();

        String message = String.format(
                "Project: %s  Branch: %s  Commit: %s  Triggered by: %s",
                req.getProjectName(), req.getBranch(),
                req.getCommitHash() != null ? req.getCommitHash() : "N/A",
                req.getTriggeredBy());

        // Resolve recipient: use request field if provided, else look up admins, else default
        String recipient = resolveRecipient(req.getRecipientEmail());

        Notification n = Notification.builder()
                .eventType(req.getEventType())
                .channel(NotificationChannel.EMAIL)
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .pipelineExecutionId(req.getPipelineExecutionId())
                .projectId(req.getProjectId())
                .sourceService("pipeline-service")
                .build();

        n = deliverNotification(n);

        try { slackService.send("[" + req.getEventType() + "] " + subject); }
        catch (Exception e) { log.warn("Slack failed (non-blocking): {}", e.getMessage()); }

        return toResponse(notificationRepository.save(n));
    }

    @Override
    public NotificationResponse handleSecurityAlert(SecurityAlertRequest req) {
        String subject = "[DevSecOps] Security Alert — "
                + req.getEventType().name() + " — " + req.getProjectName();

        String message = String.format(
                "Project: %s  Score: %.1f  Critical: %d  High: %d",
                req.getProjectName(),
                req.getSecurityScore()  != null ? req.getSecurityScore()  : 0.0,
                req.getCriticalCount()  != null ? req.getCriticalCount()  : 0,
                req.getHighCount()      != null ? req.getHighCount()      : 0);

        String recipient = resolveRecipient(null);

        Notification n = Notification.builder()
                .eventType(req.getEventType())
                .channel(NotificationChannel.EMAIL)
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .projectId(req.getProjectId())
                .pipelineExecutionId(req.getExecutionId())
                .sourceService("security-service")
                .build();

        n = deliverNotification(n);

        try { slackService.send("[SECURITY] " + subject); }
        catch (Exception e) { log.warn("Slack alert failed: {}", e.getMessage()); }

        return toResponse(notificationRepository.save(n));
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    @Override
    public List<NotificationResponse> getBySourceService(String src) {
        return notificationRepository.findBySourceService(src).stream()
                .map(this::toResponse).toList();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the actual recipient email:
     * 1. Use explicit recipientEmail from request if provided
     * 2. Fetch admin emails from Auth-Service
     * 3. Fall back to configured default
     */
    private String resolveRecipient(String requestEmail) {
        if (requestEmail != null && !requestEmail.isBlank()) {
            return requestEmail;
        }
        List<String> adminEmails = userClient.getAdminEmails();
        if (!adminEmails.isEmpty()) {
            return adminEmails.get(0); // Primary admin gets the notification
        }
        log.warn("No admin emails found, using default recipient: {}", defaultRecipient);
        return defaultRecipient;
    }

    /**
     * Attempts email delivery.
     * - Email disabled → SENT (notification is stored and visible in the UI — that IS a delivery)
     * - Email enabled + sent OK → SENT
     * - Email enabled + send failed → FAILED
     */
    private Notification deliverNotification(Notification n) {
        if (!emailEnabled) {
            // Email is intentionally off — notification is still "delivered" to the platform
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(LocalDateTime.now());
            log.info("Email delivery disabled — notification saved as SENT for: {}", n.getRecipient());
            return n;
        }
        try {
            emailService.send(n.getRecipient(), n.getSubject(), n.getMessage());
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(LocalDateTime.now());
            log.info("Email sent to {}", n.getRecipient());
        } catch (Exception e) {
            n.setStatus(NotificationStatus.FAILED);
            n.setErrorMessage(e.getMessage());
            log.error("Email failed to {}: {}", n.getRecipient(), e.getMessage());
        }
        return n;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .eventType(n.getEventType())
                .channel(n.getChannel())
                .status(n.getStatus())
                .recipient(n.getRecipient())
                .message(n.getMessage())
                .projectId(n.getProjectId())
                .pipelineExecutionId(n.getPipelineExecutionId())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}
