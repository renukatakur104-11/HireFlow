package com.hireflow.modules.student;

import com.hireflow.common.exception.BadRequestException;
import com.hireflow.common.exception.ForbiddenException;
import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.modules.storage.FileStorageService;
import com.hireflow.modules.student.dto.ResumeUploadResponse;
import com.hireflow.modules.student.dto.StudentProfileDto;
import com.hireflow.modules.student.dto.StudentProfileUpdateRequest;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final FileStorageService fileStorageService;

    @Value("${hireflow.storage.upload-dir:./uploads/resumes}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public StudentProfileDto getMyProfile(UserPrincipal principal) {
        StudentProfile profile = getProfileByUserId(principal.getId());
        return toDto(profile);
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getProfileById(Long profileId, UserPrincipal principal) {
        StudentProfile profile = studentProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile", "id", profileId));
        // Ownership check: students can only see own profile; admins can see all
        if (!principal.isAdmin() && !profile.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Access denied: You can only view your own profile.");
        }
        return toDto(profile);
    }

    @Transactional
    public StudentProfileDto updateMyProfile(StudentProfileUpdateRequest req, UserPrincipal principal) {
        StudentProfile profile = getProfileByUserId(principal.getId());

        if (req.getFullName() != null && !req.getFullName().isBlank()) profile.setFullName(req.getFullName().trim());
        if (req.getPhone() != null && !req.getPhone().isBlank()) profile.setPhone(req.getPhone().trim());
        if (req.getBranch() != null && !req.getBranch().isBlank()) profile.setBranch(req.getBranch().trim());
        if (req.getGraduationYear() != null) profile.setGraduationYear(req.getGraduationYear());
        if (req.getCgpa() != null) profile.setCgpa(req.getCgpa());
        if (req.getBacklogs() != null) profile.setBacklogs(req.getBacklogs());

        if (req.getSkills() != null) {
            Set<Skill> skills = resolveSkills(req.getSkills());
            profile.setSkills(skills);
        }

        profile.recomputeCompleteness();
        return toDto(studentProfileRepository.save(profile));
    }

    @Transactional
    public ResumeUploadResponse uploadResume(MultipartFile file, UserPrincipal principal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a PDF file to upload.");
        }

        StudentProfile profile = getProfileByUserId(principal.getId());

        // Delete old resume if present
        if (profile.getResumeFilePath() != null) {
            fileStorageService.deleteResume(profile.getResumeFilePath());
        }

        String storedFilename = fileStorageService.storeResume(file);

        profile.setResumeFilePath(storedFilename);
        profile.setResumeOriginalName(file.getOriginalFilename());
        profile.setResumeUploadedAt(LocalDateTime.now());
        profile.recomputeCompleteness();
        studentProfileRepository.save(profile);

        return ResumeUploadResponse.builder()
                .fileName(storedFilename)
                .originalName(file.getOriginalFilename())
                .sizeBytes(file.getSize())
                .message("Resume uploaded successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public StudentProfile getProfileEntityByUserId(Long userId) {
        return getProfileByUserId(userId);
    }

    public java.nio.file.Path getResumePath(String filename) {
        return fileStorageService.getResumePath(filename);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private StudentProfile getProfileByUserId(Long userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        Set<Skill> skills = new HashSet<>();
        for (String name : skillNames) {
            String clean = name.trim();
            if (!clean.isBlank()) {
                Skill skill = skillRepository.findByNameIgnoreCase(clean)
                        .orElseGet(() -> skillRepository.save(Skill.builder().name(clean).build()));
                skills.add(skill);
            }
        }
        return skills;
    }

    public StudentProfileDto toDto(StudentProfile p) {
        int completeness = computeCompleteness(p);
        return StudentProfileDto.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .email(p.getUser().getEmail())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .branch(p.getBranch())
                .graduationYear(p.getGraduationYear())
                .cgpa(p.getCgpa())
                .backlogs(p.getBacklogs())
                .skills(p.getSkills().stream().map(Skill::getName).sorted().collect(Collectors.toList()))
                .profileComplete(p.isProfileComplete())
                .completenessPercent(completeness)
                .hasResume(p.getResumeFilePath() != null)
                .resumeOriginalName(p.getResumeOriginalName())
                .resumeUploadedAt(p.getResumeUploadedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private int computeCompleteness(StudentProfile p) {
        int score = 0;
        if (p.getFullName() != null && !p.getFullName().isBlank()) score += 20;
        if (p.getPhone() != null && !p.getPhone().isBlank()) score += 10;
        if (p.getBranch() != null && !p.getBranch().isBlank()) score += 10;
        if (p.getGraduationYear() != null) score += 10;
        if (p.getCgpa() != null) score += 15;
        if (p.getSkills() != null && !p.getSkills().isEmpty()) score += 15;
        if (p.getResumeFilePath() != null && !p.getResumeFilePath().isBlank()) score += 20;
        return score;
    }
}
