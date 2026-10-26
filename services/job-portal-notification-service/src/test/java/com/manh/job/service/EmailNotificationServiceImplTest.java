package com.manh.job.service;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.domain.NotificationDeliveryStatus;
import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import com.manh.job.modal.NotificationLog;
import com.manh.job.repository.NotificationLogRepository;
import com.manh.job.service.impl.EmailNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private NotificationLogRepository notificationLogRepository;

    private EmailNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailNotificationServiceImpl(mailSender, notificationLogRepository);
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@test.local");
    }

    @Test
    void skipsWhenEmailMissing() {
        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.builder()
                .applicationId(1L)
                .candidateId(2L)
                .previousStatus(ApplicationStatus.PENDING)
                .newStatus(ApplicationStatus.SHORTLISTED)
                .changedAt(LocalDateTime.now())
                .build();

        service.handleStatusChanged(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertEquals(NotificationDeliveryStatus.SKIPPED, captor.getValue().getDeliveryStatus());
    }
}
