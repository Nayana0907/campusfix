package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;

@Service
public class VerificationReminderService {

    private static final Logger log = LoggerFactory.getLogger(VerificationReminderService.class);

    private final ComplaintVerificationRepository verificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.verification.reminders-enabled:true}")
    private boolean remindersEnabled;

    @Value("${app.verification.reminder-interval-hours:24}")
    private long reminderIntervalHours;

    @Value("${app.verification.timeout-hours:72}")
    private long timeoutHours;

    public VerificationReminderService(
            ComplaintVerificationRepository verificationRepository,
            ApplicationEventPublisher eventPublisher) {
        this.verificationRepository = verificationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Periodically inspects complaints awaiting verification and issues deduplicated reminders.
     * Never auto-closes complaints on timeout; reminders keep users informed while maintaining auditability.
     */
    @Scheduled(fixedDelayString = "${app.verification.reminder-check-interval-ms:60000}")
    @Transactional
    public void processPendingVerificationReminders() {
        if (!remindersEnabled) {
            return;
        }

        List<ComplaintVerificationEntity> pendingCycles = verificationRepository.findByVerificationStatus(VerificationStatus.PENDING);
        if (pendingCycles.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (ComplaintVerificationEntity cycle : pendingCycles) {
            ComplaintEntity complaint = cycle.getComplaint();
            if (complaint == null || complaint.getStatus() != ComplaintStatus.RESOLVED || complaint.getReporter() == null) {
                continue;
            }

            LocalDateTime baseline = cycle.getLastReminderSentAt() != null
                    ? cycle.getLastReminderSentAt()
                    : cycle.getResolvedAt();

            if (baseline == null) {
                baseline = cycle.getCreatedAt();
            }

            LocalDateTime reminderDueTime = baseline.plusHours(reminderIntervalHours);

            if (now.isAfter(reminderDueTime)) {
                cycle.setLastReminderSentAt(now);
                cycle.setReminderCount(cycle.getReminderCount() + 1);
                verificationRepository.save(cycle);

                log.info("Issuing reminder #{} for complaint {} (pending in cycle #{})",
                        cycle.getReminderCount(), complaint.getReferenceNumber(), cycle.getCycleNumber());

                eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                        complaint.getId(),
                        complaint.getReferenceNumber(),
                        complaint.getReporter().getId(),
                        complaint.getReporter().getEmail(),
                        complaint.getReporter().getName(),
                        "VERIFICATION_REMINDER",
                        "Reminder #" + cycle.getReminderCount() + ": Please verify resolution for Complaint #" + complaint.getReferenceNumber(),
                        String.format("Complaint '%s' has been marked resolved and is awaiting your verification. Please confirm or reject the repair.",
                                complaint.getTitle())
                ));
            }
        }
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public void setReminderIntervalHours(long reminderIntervalHours) {
        this.reminderIntervalHours = reminderIntervalHours;
    }
}
