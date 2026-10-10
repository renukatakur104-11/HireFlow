package com.hireflow.modules.notification;

import com.hireflow.modules.application.Application;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.application.ApplicationStatus;
import com.hireflow.modules.audit.AuditService;
import com.hireflow.modules.drive.Drive;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.modules.drive.DriveStatus;
import com.hireflow.modules.drive.EligibilityResult;
import com.hireflow.modules.drive.EligibilityService;
import com.hireflow.modules.notification.dto.BroadcastRequest;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationRecipientRepository recipientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private DriveRepository driveRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private NotificationService notificationService;

    private User adminUser;
    private User studentUser1;
    private User studentUser2;
    private StudentProfile studentProfile1;
    private StudentProfile studentProfile2;
    private Drive testDrive;
    private Application testApplication;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).email("admin@hireflow.edu").role(Role.ADMIN).active(true).build();
        studentUser1 = User.builder().id(10L).email("alice@college.edu").role(Role.STUDENT).active(true).build();
        studentUser2 = User.builder().id(20L).email("bob@college.edu").role(Role.STUDENT).active(true).build();

        studentProfile1 = StudentProfile.builder()
                .id(100L)
                .user(studentUser1)
                .fullName("Alice Smith")
                .branch("Computer Science")
                .cgpa(new BigDecimal("8.50"))
                .build();

        studentProfile2 = StudentProfile.builder()
                .id(200L)
                .user(studentUser2)
                .fullName("Bob Jones")
                .branch("Mechanical Engineering")
                .cgpa(new BigDecimal("6.20"))
                .build();

        testDrive = Drive.builder()
                .id(50L)
                .companyName("Acme Corp")
                .roleTitle("Software Engineer")
                .packageLpa(new BigDecimal("12.50"))
                .status(DriveStatus.OPEN)
                .applicationDeadline(LocalDateTime.now().plusDays(5))
                .driveDate(LocalDateTime.now().plusDays(10))
                .build();

        testApplication = Application.builder()
                .id(500L)
                .drive(testDrive)
                .studentProfile(studentProfile1)
                .currentStatus(ApplicationStatus.REGISTERED)
                .build();
    }

    // ── Workflow 1: Drive Publishing Notifications ───────────────────────────

    @Test
    @DisplayName("Workflow 1: notifyDrivePublished sends notifications only to eligible students")
    void testNotifyDrivePublished_SendsOnlyToEligibleStudents() {
        when(notificationRepository.existsByDedupKey("DRIVE_PUBLISHED:50")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1001L);
            return n;
        });
        when(studentProfileRepository.findAll()).thenReturn(List.of(studentProfile1, studentProfile2));

        // Alice is eligible, Bob is ineligible
        when(eligibilityService.evaluateEligibilityWithoutApplicationCheck(testDrive, studentProfile1))
                .thenReturn(EligibilityResult.pass());
        when(eligibilityService.evaluateEligibilityWithoutApplicationCheck(testDrive, studentProfile2))
                .thenReturn(EligibilityResult.fail(List.of("Branch not eligible")));

        notificationService.notifyDrivePublished(testDrive, adminUser);

        // Verify Notification created
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotif = notifCaptor.getValue();
        assertThat(savedNotif.getType()).isEqualTo(NotificationType.DRIVE_PUBLISHED);
        assertThat(savedNotif.getTargetDrive().getId()).isEqualTo(50L);
        assertThat(savedNotif.getTitle()).contains("Acme Corp is hiring!");

        // Verify only Alice receives the recipient record
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRecipient>> recipCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(recipCaptor.capture());
        List<NotificationRecipient> recipients = recipCaptor.getValue();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getUser().getId()).isEqualTo(studentUser1.getId());
    }

    @Test
    @DisplayName("Workflow 1: notifyDrivePublished deduplicates and does not re-send if already published")
    void testNotifyDrivePublished_DeduplicationPreventsDuplicates() {
        when(notificationRepository.existsByDedupKey("DRIVE_PUBLISHED:50")).thenReturn(true);

        notificationService.notifyDrivePublished(testDrive, adminUser);

        verify(notificationRepository, never()).save(any());
        verify(recipientRepository, never()).saveAll(any());
    }

    // ── Workflow 2: Application Status Change Notifications ──────────────────

    @Test
    @DisplayName("Workflow 2: notifyStatusChanged notifies the affected student")
    void testNotifyStatusChanged_DispatchesToApplicationStudent() {
        testApplication.setCurrentStatus(ApplicationStatus.TECHNICAL_INTERVIEW);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1002L);
            return n;
        });

        notificationService.notifyStatusChanged(testApplication);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotif = notifCaptor.getValue();
        assertThat(savedNotif.getType()).isEqualTo(NotificationType.STATUS_CHANGED);
        assertThat(savedNotif.getMessage()).contains("TECHNICAL_INTERVIEW");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRecipient>> recipCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(recipCaptor.capture());
        List<NotificationRecipient> recipients = recipCaptor.getValue();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getUser().getId()).isEqualTo(studentUser1.getId());
    }

    @Test
    @DisplayName("Workflow 2: notifyApplicationConfirmed sends confirmation on apply and avoids duplicates")
    void testNotifyApplicationConfirmed_DispatchesAndDeduplicates() {
        when(notificationRepository.existsByDedupKey("APP_CONFIRMED:500")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1003L);
            return n;
        });

        notificationService.notifyApplicationConfirmed(testApplication);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotif = notifCaptor.getValue();
        assertThat(savedNotif.getType()).isEqualTo(NotificationType.APPLICATION_CONFIRMED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRecipient>> recipCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(recipCaptor.capture());
        assertThat(recipCaptor.getValue()).hasSize(1);
    }

    // ── Workflow 3: Deadline Reminders & Deduplication ───────────────────────

    @Test
    @DisplayName("Workflow 3: sendDeadlineReminders finds drives closing in <24h, sets URGENT priority, and deduplicates")
    void testSendDeadlineReminders_DispatchesUrgentAlertsAndDeduplicates() {
        LocalDateTime closingSoonDeadline = LocalDateTime.now().plusHours(12);
        Drive closingDrive = Drive.builder()
                .id(77L)
                .companyName("Beta Labs")
                .roleTitle("Backend Intern")
                .applicationDeadline(closingSoonDeadline)
                .build();

        when(driveRepository.findActiveDrives(any(LocalDateTime.class))).thenReturn(List.of(closingDrive));
        when(notificationRepository.existsByDedupKey(startsWith("DEADLINE_24H:77:"))).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findAll()).thenReturn(List.of(studentProfile1));
        when(eligibilityService.evaluateEligibilityWithoutApplicationCheck(eq(closingDrive), eq(studentProfile1)))
                .thenReturn(EligibilityResult.pass());

        notificationService.sendDeadlineReminders();

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotif = notifCaptor.getValue();
        assertThat(savedNotif.getPriority()).isEqualTo(NotificationPriority.URGENT);
        assertThat(savedNotif.getType()).isEqualTo(NotificationType.DEADLINE_APPROACHING);
        assertThat(savedNotif.getTitle()).contains("Beta Labs");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationRecipient>> recipCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(recipCaptor.capture());
        assertThat(recipCaptor.getValue().get(0).getUser().getId()).isEqualTo(studentUser1.getId());
    }

    // ── Workflow 4: Admin Broadcast Targeting ────────────────────────────────

    @Test
    @DisplayName("Workflow 4: broadcast to ALL students targets active students and logs audit")
    void testBroadcast_AllStudents_TargetsActiveStudentsAndLogsAudit() {
        UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser, studentUser1, studentUser2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        BroadcastRequest req = BroadcastRequest.builder()
                .title("Campus Placement Orientation")
                .message("Mandatory orientation session tomorrow at 10 AM.")
                .priority(NotificationPriority.NORMAL)
                .audience("ALL")
                .build();

        int sentCount = notificationService.broadcast(req, adminPrincipal);

        assertThat(sentCount).isEqualTo(2); // studentUser1 and studentUser2 (admin filtered out)
        verify(auditService).logNotificationBroadcast(eq(1L), eq("ALL"), eq(2), any());
    }

    @Test
    @DisplayName("Workflow 4: broadcast to DRIVE_APPLICANTS targets only students who applied")
    void testBroadcast_DriveApplicants_TargetsOnlyApplicants() {
        UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Page<Application> appPage = new PageImpl<>(List.of(testApplication));
        when(applicationRepository.findByDriveId(eq(50L), any(Pageable.class))).thenReturn(appPage);

        BroadcastRequest req = BroadcastRequest.builder()
                .title("Drive Venue Updated")
                .message("Test will be conducted in Lab 3.")
                .priority(NotificationPriority.URGENT)
                .audience("DRIVE_APPLICANTS")
                .driveId(50L)
                .build();

        int sentCount = notificationService.broadcast(req, adminPrincipal);

        assertThat(sentCount).isEqualTo(1);
        verify(auditService).logNotificationBroadcast(eq(1L), eq("DRIVE_APPLICANTS"), eq(1), any());
    }

    // ── Workflow 5: Unread Count and Mark-as-Read ─────────────────────────────

    @Test
    @DisplayName("Workflow 5: getUnreadCount returns accurate unread count for user")
    void testGetUnreadCount() {
        UserPrincipal principal = UserPrincipal.create(studentUser1);
        when(recipientRepository.countByUserIdAndReadFalse(10L)).thenReturn(4L);

        long count = notificationService.getUnreadCount(principal);
        assertThat(count).isEqualTo(4L);
    }

    @Test
    @DisplayName("Workflow 5: markRead marks recipient record as read and sets timestamp")
    void testMarkRead_MarksAsRead() {
        UserPrincipal principal = UserPrincipal.create(studentUser1);
        NotificationRecipient recipient = NotificationRecipient.builder()
                .id(999L)
                .user(studentUser1)
                .read(false)
                .build();

        when(recipientRepository.findByNotificationIdAndUserId(100L, 10L)).thenReturn(Optional.of(recipient));

        notificationService.markRead(100L, principal);

        assertThat(recipient.isRead()).isTrue();
        assertThat(recipient.getReadAt()).isNotNull();
        verify(recipientRepository).save(recipient);
    }

    @Test
    @DisplayName("Workflow 5: markAllRead marks all unread notifications read for the user")
    void testMarkAllRead() {
        UserPrincipal principal = UserPrincipal.create(studentUser1);
        when(recipientRepository.markAllReadForUser(eq(10L), any(LocalDateTime.class))).thenReturn(3);

        int updated = notificationService.markAllRead(principal);
        assertThat(updated).isEqualTo(3);
    }
}
