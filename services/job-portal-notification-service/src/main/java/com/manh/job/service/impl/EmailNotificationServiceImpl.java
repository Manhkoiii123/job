package com.manh.job.service.impl;

import com.manh.job.domain.NotificationDeliveryStatus;
import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import com.manh.job.modal.NotificationLog;
import com.manh.job.repository.NotificationLogRepository;
import com.manh.job.service.EmailNotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${job.notification.mail-enabled:true}")
    private boolean mailEnabled;

    @Value("${job.notification.from:noreply@jobportal.local}")
    private String fromAddress;

    @Override
    public void handleStatusChanged(ApplicationStatusChangedEvent event) {
        String subject = buildSubject(event);
        String html = buildHtmlBody(event);

        if (event.getCandidateEmail() == null || event.getCandidateEmail().isBlank()) {
            saveLog(event, subject, html, NotificationDeliveryStatus.SKIPPED, "Missing candidate email");
            return;
        }

        if (!mailEnabled) {
            log.info("Mail disabled — skip send to {}", event.getCandidateEmail());
            saveLog(event, subject, html, NotificationDeliveryStatus.SKIPPED, "mail-enabled=false");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(event.getCandidateEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            saveLog(event, subject, html, NotificationDeliveryStatus.SENT, null);
            log.info("Email SENT applicationId={} to={}", event.getApplicationId(), event.getCandidateEmail());
        } catch (Exception ex) {
            saveLog(event, subject, html, NotificationDeliveryStatus.FAILED, ex.getMessage());
            log.error("Email FAILED applicationId={}: {}", event.getApplicationId(), ex.getMessage());
        }
    }

    private String buildSubject(ApplicationStatusChangedEvent event) {
        return "Application update: " + nullToDash(event.getJobTitle())
                + " (" + event.getPreviousStatus() + " → " + event.getNewStatus() + ")";
    }

    private String buildHtmlBody(ApplicationStatusChangedEvent event) {
        return """
                <html><body style="font-family:Arial,sans-serif;line-height:1.5">
                  <p>Hi %s,</p>
                  <p>Your application for <strong>%s</strong> at <strong>%s</strong> has been updated.</p>
                  <p>Status changed from <strong>%s</strong> → <strong>%s</strong>.</p>
                  <p>Application ID: %s</p>
                  <p>Thank you,<br/>Job Portal</p>
                </body></html>
                """.formatted(
                nullToDash(event.getCandidateName()),
                nullToDash(event.getJobTitle()),
                nullToDash(event.getCompanyName()),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getApplicationId()
        );
    }

    private void saveLog(
            ApplicationStatusChangedEvent event,
            String subject,
            String html,
            NotificationDeliveryStatus status,
            String error
    ) {
        String preview = html.length() > 500 ? html.substring(0, 500) : html;
        notificationLogRepository.save(NotificationLog.builder()
                .applicationId(event.getApplicationId())
                .candidateId(event.getCandidateId())
                .recipientEmail(event.getCandidateEmail())
                .subject(subject)
                .bodyPreview(preview)
                .previousStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .deliveryStatus(status)
                .errorMessage(error)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
