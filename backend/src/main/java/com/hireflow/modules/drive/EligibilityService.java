package com.hireflow.modules.drive;

import com.hireflow.modules.application.Application;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.student.Skill;
import com.hireflow.modules.student.StudentProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * THE authoritative eligibility engine for HireFlow.
 * All eligibility checks go through this single service.
 * DO NOT bypass for any reason — AI matching is advisory only.
 */
@Service
@RequiredArgsConstructor
public class EligibilityService {

    private final ApplicationRepository applicationRepository;

    /**
     * Evaluate all 7 deterministic eligibility rules.
     * Returns a result with reasons if ineligible.
     */
    public EligibilityResult evaluateEligibility(Drive drive, StudentProfile student) {
        List<String> reasons = new ArrayList<>();

        // Rule 1: Drive must be OPEN
        if (drive.getStatus() != DriveStatus.OPEN) {
            reasons.add("This drive is not currently open for applications (status: " + drive.getStatus() + ").");
        }

        // Rule 2: Drive must not be deleted
        if (drive.isDeleted()) {
            reasons.add("This drive is no longer available.");
        }

        // Rule 3: Application deadline must not have passed
        if (LocalDateTime.now().isAfter(drive.getApplicationDeadline())) {
            reasons.add("The application deadline has passed (" +
                    drive.getApplicationDeadline().toLocalDate() + ").");
        }

        // Rule 4: Student's branch must be in allowed branches (if branches configured)
        if (drive.getAllowedBranches() != null && !drive.getAllowedBranches().isEmpty()) {
            boolean branchAllowed = drive.getAllowedBranches().stream()
                    .anyMatch(b -> b.equalsIgnoreCase(student.getBranch()));
            if (!branchAllowed) {
                reasons.add("Your branch (" + student.getBranch() + ") is not eligible for this drive. " +
                        "Allowed branches: " + String.join(", ", drive.getAllowedBranches()) + ".");
            }
        }

        // Rule 5: Student CGPA >= drive minCgpa
        if (drive.getMinCgpa() != null && student.getCgpa() != null) {
            if (student.getCgpa().compareTo(drive.getMinCgpa()) < 0) {
                reasons.add(String.format("Your CGPA %.2f is below the required minimum of %.2f.",
                        student.getCgpa().doubleValue(), drive.getMinCgpa().doubleValue()));
            }
        }

        // Rule 6: Backlogs check (student backlogs <= drive.maxBacklogs)
        if (drive.getMaxBacklogs() != null && student.getBacklogs() != null) {
            if (student.getBacklogs() > drive.getMaxBacklogs()) {
                reasons.add(String.format("You have %d active backlog(s); this drive allows a maximum of %d.",
                        student.getBacklogs(), drive.getMaxBacklogs()));
            }
        }

        // Rule 7: Student must not have already applied
        boolean alreadyApplied = applicationRepository
                .existsByDriveIdAndStudentProfileId(drive.getId(), student.getId());
        if (alreadyApplied) {
            reasons.add("You have already applied to this drive.");
        }

        return reasons.isEmpty() ? EligibilityResult.pass() : EligibilityResult.fail(reasons);
    }

    /**
     * Quick eligibility check without already-applied check.
     * Used for notification targeting and listing eligible drives.
     */
    public EligibilityResult evaluateEligibilityWithoutApplicationCheck(Drive drive, StudentProfile student) {
        List<String> reasons = new ArrayList<>();

        if (drive.getStatus() != DriveStatus.OPEN) {
            reasons.add("Drive is not open.");
        }
        if (drive.isDeleted()) {
            reasons.add("Drive is deleted.");
        }
        if (LocalDateTime.now().isAfter(drive.getApplicationDeadline())) {
            reasons.add("Deadline passed.");
        }
        if (drive.getAllowedBranches() != null && !drive.getAllowedBranches().isEmpty()) {
            boolean branchAllowed = drive.getAllowedBranches().stream()
                    .anyMatch(b -> b.equalsIgnoreCase(student.getBranch()));
            if (!branchAllowed) reasons.add("Branch not eligible.");
        }
        if (drive.getMinCgpa() != null && student.getCgpa() != null) {
            if (student.getCgpa().compareTo(drive.getMinCgpa()) < 0) {
                reasons.add("CGPA below requirement.");
            }
        }
        if (drive.getMaxBacklogs() != null && student.getBacklogs() != null) {
            if (student.getBacklogs() > drive.getMaxBacklogs()) reasons.add("Too many backlogs.");
        }

        return reasons.isEmpty() ? EligibilityResult.pass() : EligibilityResult.fail(reasons);
    }
}
