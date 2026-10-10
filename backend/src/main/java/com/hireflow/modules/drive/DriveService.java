package com.hireflow.modules.drive;

import com.hireflow.common.exception.BadRequestException;
import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.drive.dto.*;
import com.hireflow.modules.notification.NotificationService;
import com.hireflow.modules.student.Skill;
import com.hireflow.modules.student.SkillRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveService {

    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final EligibilityService eligibilityService;
    private final NotificationService notificationService;
    private final com.hireflow.modules.audit.AuditService auditService;

    @Transactional
    public DriveDto createDrive(DriveCreateRequest req, UserPrincipal adminPrincipal) {
        User admin = userRepository.findById(adminPrincipal.getId()).orElseThrow();

        Drive drive = Drive.builder()
                .companyName(req.getCompanyName().trim())
                .roleTitle(req.getRoleTitle().trim())
                .description(req.getDescription())
                .packageLpa(req.getPackageLpa())
                .packageDetails(req.getPackageDetails())
                .location(req.getLocation().trim())
                .minCgpa(req.getMinCgpa() != null ? req.getMinCgpa() : BigDecimal.ZERO)
                .maxBacklogs(req.getMaxBacklogs() != null ? req.getMaxBacklogs() : 0)
                .driveDate(req.getDriveDate())
                .applicationDeadline(req.getApplicationDeadline())
                .status(req.getStatus() != null ? req.getStatus() : DriveStatus.DRAFT)
                .createdBy(admin)
                .build();

        if (req.getAllowedBranches() != null) {
            drive.setAllowedBranches(new HashSet<>(req.getAllowedBranches()));
        }
        if (req.getRequiredSkills() != null) {
            drive.setRequiredSkills(resolveSkills(req.getRequiredSkills()));
        }

        drive = driveRepository.save(drive);
        if (req.getRounds() != null) {
            Drive finalDrive = drive;
            Set<DriveRound> rounds = req.getRounds().stream().map(r -> DriveRound.builder()
                    .drive(finalDrive)
                    .roundOrder(r.getRoundOrder())
                    .roundName(r.getRoundName())
                    .statusCode(r.getStatusCode())
                    .description(r.getDescription())
                    .build()).collect(Collectors.toSet());
            drive.setRounds(rounds);
            drive = driveRepository.save(drive);
        }

        try {
            auditService.logDriveCreated(admin.getId(), drive.getId(), drive.getCompanyName(), drive.getRoleTitle(), null);
        } catch (Exception e) {
            log.warn("Failed to audit drive creation: {}", e.getMessage());
        }

        if (drive.getStatus() == DriveStatus.OPEN) {
            try {
                notificationService.notifyDrivePublished(drive, admin);
            } catch (Exception e) {
                log.warn("Failed to send drive published notification: {}", e.getMessage());
            }
        }

        return toDto(drive, null);
    }

    @Transactional
    public DriveDto updateDrive(Long driveId, DriveCreateRequest req, UserPrincipal adminPrincipal) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", driveId));

        DriveStatus previousStatus = drive.getStatus();
        drive.setCompanyName(req.getCompanyName().trim());
        drive.setRoleTitle(req.getRoleTitle().trim());
        drive.setDescription(req.getDescription());
        drive.setPackageLpa(req.getPackageLpa());
        drive.setPackageDetails(req.getPackageDetails());
        drive.setLocation(req.getLocation().trim());
        if (req.getMinCgpa() != null) drive.setMinCgpa(req.getMinCgpa());
        if (req.getMaxBacklogs() != null) drive.setMaxBacklogs(req.getMaxBacklogs());
        drive.setDriveDate(req.getDriveDate());
        drive.setApplicationDeadline(req.getApplicationDeadline());
        if (req.getStatus() != null) drive.setStatus(req.getStatus());
        if (req.getAllowedBranches() != null) drive.setAllowedBranches(new HashSet<>(req.getAllowedBranches()));
        if (req.getRequiredSkills() != null) drive.setRequiredSkills(resolveSkills(req.getRequiredSkills()));

        Drive saved = driveRepository.save(drive);
        if (previousStatus != DriveStatus.OPEN && saved.getStatus() == DriveStatus.OPEN) {
            User admin = userRepository.findById(adminPrincipal.getId()).orElse(null);
            try {
                notificationService.notifyDrivePublished(saved, admin);
            } catch (Exception e) {
                log.warn("Failed to send drive published notification: {}", e.getMessage());
            }
        }

        return toDto(saved, null);
    }

    @Transactional
    public void deleteDrive(Long driveId, UserPrincipal adminPrincipal) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", driveId));
        long appCount = applicationRepository.countByDriveId(driveId);
        if (appCount > 0) {
            // Soft-close instead of hard delete if applications exist
            drive.setStatus(DriveStatus.CLOSED);
            drive.setDeleted(true);
        } else {
            drive.setDeleted(true);
        }
        driveRepository.save(drive);

        if (adminPrincipal != null) {
            try {
                auditService.logDriveDeleted(adminPrincipal.getId(), driveId, drive.getCompanyName(), null);
            } catch (Exception e) {
                log.warn("Failed to audit drive deletion: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public DriveDto updateStatus(Long driveId, DriveStatus newStatus) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", driveId));
        DriveStatus previousStatus = drive.getStatus();
        drive.setStatus(newStatus);
        Drive saved = driveRepository.save(drive);

        if (previousStatus != DriveStatus.OPEN && newStatus == DriveStatus.OPEN) {
            try {
                notificationService.notifyDrivePublished(saved, null);
            } catch (Exception e) {
                log.warn("Failed to send drive published notification: {}", e.getMessage());
            }
        }

        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<DriveDto> listDrives(String search, DriveStatus status, BigDecimal minPackage, BigDecimal maxPackage,
                                     BigDecimal maxCgpa, LocalDateTime deadlineFrom, LocalDateTime deadlineTo,
                                     Pageable pageable, UserPrincipal principal) {
        String searchLike = search != null ? "%" + search.toLowerCase() + "%" : null;
        Page<Drive> drives = driveRepository.searchDrives(search, searchLike, status, minPackage, maxPackage,
                maxCgpa, deadlineFrom, deadlineTo, pageable);

        StudentProfile studentProfile = null;
        if (principal != null && !principal.isAdmin()) {
            studentProfile = studentProfileRepository.findByUserId(principal.getId()).orElse(null);
        }

        final StudentProfile sp = studentProfile;
        return drives.map(d -> toDto(d, sp));
    }

    @Transactional(readOnly = true)
    public DriveDto getDriveById(Long driveId, UserPrincipal principal) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", driveId));

        StudentProfile sp = null;
        if (principal != null && !principal.isAdmin()) {
            sp = studentProfileRepository.findByUserId(principal.getId()).orElse(null);
        }
        return toDto(drive, sp);
    }

    public DriveDto toDto(Drive d, StudentProfile student) {
        long appCount = applicationRepository.countByDriveId(d.getId());
        DriveDto dto = DriveDto.builder()
                .id(d.getId())
                .companyName(d.getCompanyName())
                .roleTitle(d.getRoleTitle())
                .description(d.getDescription())
                .packageLpa(d.getPackageLpa())
                .packageDetails(d.getPackageDetails())
                .location(d.getLocation())
                .minCgpa(d.getMinCgpa())
                .maxBacklogs(d.getMaxBacklogs())
                .driveDate(d.getDriveDate())
                .applicationDeadline(d.getApplicationDeadline())
                .status(d.getStatus())
                .allowedBranches(d.getAllowedBranches() != null ? List.copyOf(d.getAllowedBranches()) : List.of())
                .requiredSkills(d.getRequiredSkills() != null ? d.getRequiredSkills().stream().map(Skill::getName).sorted().collect(Collectors.toList()) : List.of())
                .rounds(d.getRounds() != null ? d.getRounds().stream().sorted(java.util.Comparator.comparingInt(DriveRound::getRoundOrder)).map(r -> DriveRoundDto.builder().id(r.getId()).roundOrder(r.getRoundOrder()).roundName(r.getRoundName()).statusCode(r.getStatusCode()).description(r.getDescription()).build()).collect(Collectors.toList()) : List.of())
                .totalApplications((int) appCount)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();

        if (student != null) {
            EligibilityResult result = eligibilityService.evaluateEligibility(d, student);
            dto.setIsEligible(result.eligible());
            dto.setHasApplied(!result.eligible() && result.reasons().stream().anyMatch(r -> r.contains("already applied")));
            dto.setEligibilityReasons(result.reasons());
        }
        return dto;
    }

    private Set<Skill> resolveSkills(List<String> names) {
        Set<Skill> skills = new HashSet<>();
        for (String name : names) {
            String clean = name.trim();
            if (!clean.isBlank()) {
                skills.add(skillRepository.findByNameIgnoreCase(clean)
                        .orElseGet(() -> skillRepository.save(Skill.builder().name(clean).build())));
            }
        }
        return skills;
    }
}
