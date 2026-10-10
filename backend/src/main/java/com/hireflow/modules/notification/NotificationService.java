package com.hireflow.modules.notification;

import com.hireflow.modules.application.Application;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.drive.Drive;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.modules.drive.EligibilityService;
import com.hireflow.modules.notification.dto.BroadcastRequest;
import com.hireflow.modules.notification.dto.NotificationDto;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final EligibilityService eligibilityService;
    private final com.hireflow.modules.audit.AuditService auditService;

    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(UserPrincipal principal, Pageable pageable) {
        return recipientRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal principal) {
        return recipientRepository.countByUserIdAndReadFalse(principal.getId());
    }

    @Transactional
    public void markRead(Long notificationOrRecipientId, UserPrincipal principal) {
        recipientRepository.findByNotificationIdAndUserId(notificationOrRecipientId, principal.getId())
                .or(() -> recipientRepository.findById(notificationOrRecipientId)
                        .filter(r -> r.getUser().getId().equals(principal.getId())))
                .ifPresent(r -> {
                    r.setRead(true);
                    r.setReadAt(LocalDateTime.now());
                    recipientRepository.save(r);
                });
    }

    @Transactional
    public int markAllRead(UserPrincipal principal) {
        return recipientRepository.markAllReadForUser(principal.getId(), LocalDateTime.now());
    }

    @Transactional
    public void notifyDrivePublished(Drive drive, User createdBy) {
        String dedupKey = "DRIVE_PUBLISHED:" + drive.getId();
        if (notificationRepository.existsByDedupKey(dedupKey)) return;
        Notification notif = createNotification(
                drive.getCompanyName() + " is hiring!",
                "A new placement drive for " + drive.getRoleTitle() + " at " + drive.getCompanyName() +
                        " is now open. Package: " + drive.getPackageLpa() + " LPA. Deadline: " + drive.getApplicationDeadline().toLocalDate(),
                NotificationPriority.NORMAL, NotificationType.DRIVE_PUBLISHED, drive, createdBy, dedupKey);
        List<User> eligible = studentProfileRepository.findAll().stream()
                .filter(sp -> eligibilityService.evaluateEligibilityWithoutApplicationCheck(drive, sp).eligible())
                .map(sp -> sp.getUser()).collect(Collectors.toList());
        dispatchToUsers(notif, eligible);
        log.info("Drive published notification sent to {} eligible students", eligible.size());
    }

    @Transactional
    public void notifyStatusChanged(Application application) {
        String dedupKey = "STATUS_CHANGED:" + application.getId() + ":" + application.getCurrentStatus() + ":" + System.currentTimeMillis();
        Notification notif = createNotification(
                "Application status updated – " + application.getDrive().getCompanyName(),
                "Your application for " + application.getDrive().getRoleTitle() + " at " +
                        application.getDrive().getCompanyName() + " has been updated to: " + application.getCurrentStatus(),
                NotificationPriority.NORMAL, NotificationType.STATUS_CHANGED, application.getDrive(), null, dedupKey);
        dispatchToUsers(notif, List.of(application.getStudentProfile().getUser()));
    }

    @Transactional
    public void notifyApplicationConfirmed(Application application) {
        String dedupKey = "APP_CONFIRMED:" + application.getId();
        if (notificationRepository.existsByDedupKey(dedupKey)) return;
        Notification notif = createNotification(
                "Application submitted – " + application.getDrive().getCompanyName(),
                "You have successfully registered for " + application.getDrive().getRoleTitle() + " at " +
                        application.getDrive().getCompanyName() + ". Drive date: " + application.getDrive().getDriveDate().toLocalDate(),
                NotificationPriority.NORMAL, NotificationType.APPLICATION_CONFIRMED, application.getDrive(), null, dedupKey);
        dispatchToUsers(notif, List.of(application.getStudentProfile().getUser()));
    }

    @Transactional
    public int broadcast(BroadcastRequest req, UserPrincipal adminPrincipal) {
        User admin = userRepository.findById(adminPrincipal.getId()).orElseThrow();
        List<User> targets = resolveAudience(req);
        if (targets.isEmpty()) return 0;
        String dedupKey = "BROADCAST:" + admin.getId() + ":" + System.currentTimeMillis();
        Notification notif = createNotification(req.getTitle(), req.getMessage(), req.getPriority(),
                NotificationType.CUSTOM_BROADCAST, null, admin, dedupKey);
        dispatchToUsers(notif, targets);

        try {
            auditService.logNotificationBroadcast(admin.getId(),
                    req.getAudience() != null ? req.getAudience() : "ALL",
                    targets.size(), null);
        } catch (Exception e) {
            log.warn("Failed to audit notification broadcast: {}", e.getMessage());
        }

        return targets.size();
    }

    @Transactional(readOnly = true)
    public int previewCount(BroadcastRequest req) {
        return resolveAudience(req).size();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24h = now.plusHours(24);
        List<Drive> soonDrives = driveRepository.findActiveDrives(now).stream()
                .filter(d -> d.getApplicationDeadline().isAfter(now) && d.getApplicationDeadline().isBefore(in24h))
                .collect(Collectors.toList());
        for (Drive drive : soonDrives) {
            String dedupKey = "DEADLINE_24H:" + drive.getId() + ":" + now.toLocalDate();
            if (notificationRepository.existsByDedupKey(dedupKey)) continue;
            Notification notif = createNotification(
                    "⏰ Deadline approaching – " + drive.getCompanyName(),
                    "The application deadline for " + drive.getRoleTitle() + " at " + drive.getCompanyName() +
                            " closes on " + drive.getApplicationDeadline().toLocalDate() + ". Apply now!",
                    NotificationPriority.URGENT, NotificationType.DEADLINE_APPROACHING, drive, null, dedupKey);
            List<User> eligible = studentProfileRepository.findAll().stream()
                    .filter(sp -> eligibilityService.evaluateEligibilityWithoutApplicationCheck(drive, sp).eligible())
                    .map(sp -> sp.getUser()).collect(Collectors.toList());
            dispatchToUsers(notif, eligible);
        }
        if (!soonDrives.isEmpty()) log.info("Deadline reminders sent for {} drives", soonDrives.size());
    }

    private Notification createNotification(String title, String message, NotificationPriority priority,
                                             NotificationType type, Drive drive, User createdBy, String dedupKey) {
        return notificationRepository.save(Notification.builder()
                .title(title).message(message).priority(priority).type(type)
                .targetDrive(drive).createdBy(createdBy).dedupKey(dedupKey).build());
    }

    private void dispatchToUsers(Notification notif, List<User> users) {
        List<NotificationRecipient> recipients = users.stream()
                .map(u -> NotificationRecipient.builder().notification(notif).user(u).build())
                .collect(Collectors.toList());
        recipientRepository.saveAll(recipients);
    }

    private List<User> resolveAudience(BroadcastRequest req) {
        String audience = req.getAudience() != null ? req.getAudience().toUpperCase() : "ALL";
        return switch (audience) {
            case "DRIVE_APPLICANTS" -> {
                if (req.getDriveId() == null) yield List.of();
                yield applicationRepository.findByDriveId(req.getDriveId(), Pageable.unpaged())
                        .getContent().stream().map(a -> a.getStudentProfile().getUser()).distinct().collect(Collectors.toList());
            }
            case "DRIVE_ELIGIBLE" -> {
                if (req.getDriveId() == null) yield List.of();
                Drive drive = driveRepository.findByIdAndDeletedFalse(req.getDriveId()).orElse(null);
                if (drive == null) yield List.of();
                yield studentProfileRepository.findAll().stream()
                        .filter(sp -> eligibilityService.evaluateEligibilityWithoutApplicationCheck(drive, sp).eligible())
                        .map(sp -> sp.getUser()).collect(Collectors.toList());
            }
            default -> userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.STUDENT && u.isActive())
                    .collect(Collectors.toList());
        };
    }

    private NotificationDto toDto(NotificationRecipient r) {
        Notification n = r.getNotification();
        return NotificationDto.builder()
                .id(r.getId()).notificationId(n.getId()).title(n.getTitle()).message(n.getMessage())
                .priority(n.getPriority()).type(n.getType())
                .targetDriveId(n.getTargetDrive() != null ? n.getTargetDrive().getId() : null)
                .read(r.isRead()).readAt(r.getReadAt()).createdAt(r.getCreatedAt()).build();
    }
}
