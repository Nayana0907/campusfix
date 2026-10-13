package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationReminderServiceTest {

    @Mock
    private ComplaintVerificationRepository verificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VerificationReminderService reminderService;

    private ComplaintEntity complaint;
    private ComplaintVerificationEntity cycle;
    private UserEntity reporter;

    @BeforeEach
    void setUp() {
        reporter = new UserEntity();
        reporter.setId(10L);
        reporter.setName("Alice Reporter");
        reporter.setEmail("alice@campus.edu");
        reporter.setRole(Role.STUDENT);

        complaint = new ComplaintEntity();
        complaint.setId(100L);
        complaint.setReferenceNumber("CMP-20261012-REM001");
        complaint.setTitle("Broken light in hallway");
        complaint.setCategory(ComplaintCategory.ELECTRICAL);
        complaint.setPriority(ComplaintPriority.HIGH);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setVerificationStatus(VerificationStatus.PENDING);
        complaint.setReporter(reporter);

        cycle = new ComplaintVerificationEntity(
                complaint, 1, LocalDateTime.now().minusHours(30), "Replaced bulb", null
        );
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        reminderService.setRemindersEnabled(true);
        reminderService.setReminderIntervalHours(24);
    }

    @Test
    @DisplayName("processPendingVerificationReminders: Sends reminder and increments counter when due")
    void processPendingVerificationReminders_SendsReminder_WhenDue() {
        when(verificationRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of(cycle));

        reminderService.processPendingVerificationReminders();

        assertEquals(1, cycle.getReminderCount());
        assertNotNull(cycle.getLastReminderSentAt());
        verify(verificationRepository).save(cycle);
        verify(eventPublisher).publishEvent(any(ComplaintVerificationDomainEvent.class));
    }

    @Test
    @DisplayName("processPendingVerificationReminders: Skips cycle when reminder interval has not yet elapsed")
    void processPendingVerificationReminders_Skips_WhenNotDue() {
        // Resolved only 2 hours ago, interval is 24h
        cycle.setResolvedAt(LocalDateTime.now().minusHours(2));

        when(verificationRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of(cycle));

        reminderService.processPendingVerificationReminders();

        assertEquals(0, cycle.getReminderCount());
        verify(verificationRepository, never()).save(cycle);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("processPendingVerificationReminders: Does nothing when reminders are disabled")
    void processPendingVerificationReminders_Skips_WhenDisabled() {
        reminderService.setRemindersEnabled(false);

        reminderService.processPendingVerificationReminders();

        verifyNoInteractions(verificationRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("processPendingVerificationReminders: Skips complaint if operational status is not RESOLVED")
    void processPendingVerificationReminders_Skips_WhenComplaintStatusNotResolved() {
        complaint.setStatus(ComplaintStatus.CLOSED);

        when(verificationRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of(cycle));

        reminderService.processPendingVerificationReminders();

        assertEquals(0, cycle.getReminderCount());
        verify(verificationRepository, never()).save(cycle);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
