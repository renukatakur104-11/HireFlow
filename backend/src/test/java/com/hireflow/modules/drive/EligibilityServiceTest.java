package com.hireflow.modules.drive;

import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.student.Skill;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private EligibilityService eligibilityService;

    private Drive testDrive;
    private StudentProfile eligibleStudent;

    @BeforeEach
    void setUp() {
        testDrive = Drive.builder()
                .id(1L)
                .companyName("Acme Corp")
                .roleTitle("Software Engineer")
                .status(DriveStatus.OPEN)
                .deleted(false)
                .minCgpa(new BigDecimal("7.5"))
                .maxBacklogs(0)
                .applicationDeadline(LocalDateTime.now().plusDays(5))
                .allowedBranches(Set.of("Computer Science", "Information Technology"))
                .build();

        User user = User.builder().id(10L).email("student@college.edu").build();

        eligibleStudent = StudentProfile.builder()
                .id(100L)
                .user(user)
                .fullName("Priya Sharma")
                .branch("Computer Science")
                .cgpa(new BigDecimal("8.2"))
                .backlogs(0)
                .profileComplete(true)
                .skills(Set.of(Skill.builder().name("Java").build()))
                .build();
    }

    @Test
    @DisplayName("Eligible student passes all deterministic checks")
    void testEligibleStudentPasses() {
        when(applicationRepository.existsByDriveIdAndStudentProfileId(1L, 100L)).thenReturn(false);

        EligibilityResult result = eligibilityService.evaluateEligibility(testDrive, eligibleStudent);

        assertTrue(result.eligible());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    @DisplayName("Student below CGPA cutoff is deemed ineligible with clear reason")
    void testIneligibleDueToLowCgpa() {
        eligibleStudent.setCgpa(new BigDecimal("7.0"));
        when(applicationRepository.existsByDriveIdAndStudentProfileId(1L, 100L)).thenReturn(false);

        EligibilityResult result = eligibilityService.evaluateEligibility(testDrive, eligibleStudent);

        assertFalse(result.eligible());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("CGPA")));
    }

    @Test
    @DisplayName("Student with excess backlogs is deemed ineligible")
    void testIneligibleDueToBacklogs() {
        eligibleStudent.setBacklogs(2);
        when(applicationRepository.existsByDriveIdAndStudentProfileId(1L, 100L)).thenReturn(false);

        EligibilityResult result = eligibilityService.evaluateEligibility(testDrive, eligibleStudent);

        assertFalse(result.eligible());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("backlog")));
    }

    @Test
    @DisplayName("Student with non-matching branch is deemed ineligible")
    void testIneligibleDueToBranch() {
        eligibleStudent.setBranch("Civil Engineering");
        when(applicationRepository.existsByDriveIdAndStudentProfileId(1L, 100L)).thenReturn(false);

        EligibilityResult result = eligibilityService.evaluateEligibility(testDrive, eligibleStudent);

        assertFalse(result.eligible());
        assertTrue(result.reasons().stream().anyMatch(r -> r.toLowerCase().contains("branch")));
    }

    @Test
    @DisplayName("Already applied student cannot re-apply")
    void testIneligibleIfAlreadyApplied() {
        when(applicationRepository.existsByDriveIdAndStudentProfileId(1L, 100L)).thenReturn(true);

        EligibilityResult result = eligibilityService.evaluateEligibility(testDrive, eligibleStudent);

        assertFalse(result.eligible());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("already applied")));
    }
}
