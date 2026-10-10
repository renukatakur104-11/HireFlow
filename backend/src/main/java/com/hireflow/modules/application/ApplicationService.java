package com.hireflow.modules.application;

import com.hireflow.common.exception.BadRequestException;
import com.hireflow.common.exception.ForbiddenException;
import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.modules.application.dto.*;
import com.hireflow.modules.drive.Drive;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.modules.drive.EligibilityResult;
import com.hireflow.modules.drive.EligibilityService;
import com.hireflow.modules.notification.NotificationService;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final DriveRepository driveRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final EligibilityService eligibilityService;
    private final StateMachineService stateMachineService;
    private final NotificationService notificationService;
    private final com.hireflow.modules.audit.AuditService auditService;

    @Transactional
    public ApplicationDto apply(Long driveId, UserPrincipal principal) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", driveId));

        StudentProfile student = studentProfileRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new BadRequestException("Student profile not found. Please complete your profile first."));

        if (!student.isProfileComplete()) {
            throw new BadRequestException("Your profile is incomplete. Please complete your profile and upload your resume before applying.");
        }

        EligibilityResult result = eligibilityService.evaluateEligibility(drive, student);
        if (!result.eligible()) {
            throw new BadRequestException("You are not eligible for this drive: " + String.join(" ", result.reasons()));
        }

        Application application = Application.builder()
                .drive(drive)
                .studentProfile(student)
                .currentStatus(ApplicationStatus.REGISTERED)
                .build();
        application = applicationRepository.save(application);

        User adminUser = userRepository.findById(principal.getId()).orElseThrow();
        saveHistory(application, null, ApplicationStatus.REGISTERED, adminUser, "Applied to drive", false);

        try {
            notificationService.notifyApplicationConfirmed(application);
        } catch (Exception e) {
            log.warn("Failed to dispatch application confirmation notification: {}", e.getMessage());
        }

        return toDto(application);
    }

    @Transactional
    public ApplicationDto withdraw(Long applicationId, UserPrincipal principal) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // Ownership check
        if (!app.getStudentProfile().getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("You can only withdraw your own applications.");
        }

        if (app.getCurrentStatus() != ApplicationStatus.REGISTERED) {
            throw new BadRequestException("You can only withdraw an application before Round 1 begins (current status: " + app.getCurrentStatus() + ").");
        }

        ApplicationStatus prevStatus = app.getCurrentStatus();
        app.setCurrentStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(app);

        User user = userRepository.findById(principal.getId()).orElseThrow();
        saveHistory(app, prevStatus, ApplicationStatus.WITHDRAWN, user, "Student withdrew application", false);

        return toDto(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getMyApplications(UserPrincipal principal) {
        StudentProfile profile = studentProfileRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return applicationRepository.findByStudentProfileIdWithDetails(profile.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> getDriveApplicants(Long driveId, ApplicationStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return applicationRepository.findByDriveIdAndCurrentStatus(driveId, statusFilter, pageable).map(this::toDto);
        }
        return applicationRepository.findByDriveId(driveId, pageable).map(this::toDto);
    }

    @Transactional
    public ApplicationDto updateStatus(Long applicationId, ApplicationStatusUpdateRequest req, UserPrincipal adminPrincipal) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        stateMachineService.validateTransition(app.getCurrentStatus(), req.getNewStatus(), req.isOverride(), req.getRemarks());

        ApplicationStatus prev = app.getCurrentStatus();
        app.setCurrentStatus(req.getNewStatus());
        applicationRepository.save(app);

        User admin = userRepository.findById(adminPrincipal.getId()).orElseThrow();
        saveHistory(app, prev, req.getNewStatus(), admin, req.getRemarks(), req.isOverride());

        try {
            auditService.logApplicationStatusChanged(admin.getId(), applicationId,
                    prev.name(), req.getNewStatus().name(), req.isOverride(), null);
        } catch (Exception e) {
            log.warn("Failed to audit application status change: {}", e.getMessage());
        }

        try {
            notificationService.notifyStatusChanged(app);
        } catch (Exception e) {
            log.warn("Failed to dispatch status changed notification: {}", e.getMessage());
        }

        return toDto(app);
    }

    @Transactional
    public int bulkUpdateStatus(BulkStatusUpdateRequest req, UserPrincipal adminPrincipal) {
        User admin = userRepository.findById(adminPrincipal.getId()).orElseThrow();
        int updated = 0;
        for (Long appId : req.getApplicationIds()) {
            try {
                Application app = applicationRepository.findById(appId).orElse(null);
                if (app == null) continue;
                stateMachineService.validateTransition(app.getCurrentStatus(), req.getNewStatus(), req.isOverride(), req.getRemarks());
                ApplicationStatus prev = app.getCurrentStatus();
                app.setCurrentStatus(req.getNewStatus());
                applicationRepository.save(app);
                saveHistory(app, prev, req.getNewStatus(), admin, req.getRemarks(), req.isOverride());
                try {
                    auditService.logApplicationStatusChanged(admin.getId(), appId,
                            prev.name(), req.getNewStatus().name(), req.isOverride(), null);
                } catch (Exception auditEx) {
                    log.warn("Failed to audit bulk status change for app {}: {}", appId, auditEx.getMessage());
                }
                try {
                    notificationService.notifyStatusChanged(app);
                } catch (Exception notifEx) {
                    log.warn("Failed to send status notification for app {}: {}", appId, notifEx.getMessage());
                }
                updated++;
            } catch (Exception e) {
                log.warn("Could not update application {}: {}", appId, e.getMessage());
            }
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryDto> getHistory(Long applicationId) {
        return historyRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream().map(h -> ApplicationStatusHistoryDto.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .changedByEmail(h.getChangedBy().getEmail())
                        .remarks(h.getRemarks())
                        .override(h.isOverride())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void saveHistory(Application app, ApplicationStatus from, ApplicationStatus to, User changedBy, String remarks, boolean override) {
        historyRepository.save(ApplicationStatusHistory.builder()
                .application(app)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .remarks(remarks)
                .override(override)
                .build());
    }

    public ApplicationDto toDto(Application a) {
        StudentProfile sp = a.getStudentProfile();
        Drive d = a.getDrive();
        return ApplicationDto.builder()
                .id(a.getId())
                .driveId(d.getId())
                .companyName(d.getCompanyName())
                .roleTitle(d.getRoleTitle())
                .location(d.getLocation())
                .packageLpa(d.getPackageLpa())
                .studentProfileId(sp.getId())
                .studentName(sp.getFullName())
                .studentEmail(sp.getUser().getEmail())
                .branch(sp.getBranch())
                .cgpa(sp.getCgpa())
                .currentStatus(a.getCurrentStatus())
                .appliedAt(a.getAppliedAt())
                .updatedAt(a.getUpdatedAt())
                .driveDate(d.getDriveDate())
                .applicationDeadline(d.getApplicationDeadline())
                .build();
    }
}
