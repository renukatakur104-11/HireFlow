package com.hireflow.modules.student;

import com.hireflow.common.exception.BadRequestException;
import com.hireflow.common.exception.ForbiddenException;
import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.modules.storage.FileStorageService;
import com.hireflow.modules.student.dto.BulkImportResponse;
import com.hireflow.modules.student.dto.ResumeUploadResponse;
import com.hireflow.modules.student.dto.RowErrorDto;
import com.hireflow.modules.student.dto.StudentProfileDto;
import com.hireflow.modules.student.dto.StudentProfileUpdateRequest;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public BulkImportResponse bulkImportStudents(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a valid CSV file to upload.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only CSV files are supported for bulk import.");
        }

        List<RowErrorDto> errors = new ArrayList<>();
        Set<String> seenEmailsInFile = new HashSet<>();
        int totalRows = 0;
        int successfulCount = 0;
        int failedCount = 0;

        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BadRequestException("The CSV file is empty.");
            }

            List<String> headers = parseCsvLine(headerLine).stream().map(String::toLowerCase).collect(Collectors.toList());

            int nameIdx = findHeaderIndex(headers, "name", "fullname", "full name");
            int emailIdx = findHeaderIndex(headers, "email", "email address", "emailaddress");
            int phoneIdx = findHeaderIndex(headers, "phone", "mobile", "phonenumber");
            int branchIdx = findHeaderIndex(headers, "branch", "department");
            int yearIdx = findHeaderIndex(headers, "graduationyear", "year", "graduation year", "batch");
            int cgpaIdx = findHeaderIndex(headers, "cgpa", "gpa");
            int backlogsIdx = findHeaderIndex(headers, "backlogs", "active backlogs");
            int skillsIdx = findHeaderIndex(headers, "skills", "skill tags");
            int passwordIdx = findHeaderIndex(headers, "password");

            if (nameIdx == -1 || emailIdx == -1 || branchIdx == -1) {
                throw new BadRequestException("CSV header must contain at least 'Name', 'Email', and 'Branch' columns.");
            }

            String line;
            int rowNum = 1;

            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;

                totalRows++;
                List<String> cols = parseCsvLine(line);

                String name = getColValue(cols, nameIdx);
                String email = getColValue(cols, emailIdx).toLowerCase();
                String phone = getColValue(cols, phoneIdx);
                String branch = getColValue(cols, branchIdx);
                String yearStr = getColValue(cols, yearIdx);
                String cgpaStr = getColValue(cols, cgpaIdx);
                String backlogsStr = getColValue(cols, backlogsIdx);
                String skillsStr = getColValue(cols, skillsIdx);
                String password = getColValue(cols, passwordIdx);

                List<String> rowErrors = new ArrayList<>();

                if (email.isBlank()) {
                    rowErrors.add("Email is required");
                } else if (!emailPattern.matcher(email).matches()) {
                    rowErrors.add("Invalid email format: '" + email + "'");
                } else if (seenEmailsInFile.contains(email)) {
                    rowErrors.add("Duplicate email in CSV file: '" + email + "'");
                } else if (userRepository.existsByEmail(email)) {
                    rowErrors.add("Email already registered in system: '" + email + "'");
                }

                if (name.isBlank() || name.length() < 2) {
                    rowErrors.add("Full Name must be at least 2 characters");
                }

                if (branch.isBlank()) {
                    rowErrors.add("Branch is required");
                }

                Integer year = 2026;
                if (!yearStr.isBlank()) {
                    try {
                        year = Integer.parseInt(yearStr);
                        if (year < 2020 || year > 2035) {
                            rowErrors.add("Graduation Year must be between 2020 and 2035");
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add("Invalid Graduation Year: '" + yearStr + "'");
                    }
                }

                BigDecimal cgpa = BigDecimal.ZERO;
                if (!cgpaStr.isBlank()) {
                    try {
                        cgpa = new BigDecimal(cgpaStr);
                        if (cgpa.compareTo(BigDecimal.ZERO) < 0 || cgpa.compareTo(new BigDecimal("10.0")) > 0) {
                            rowErrors.add("CGPA must be between 0.00 and 10.00");
                        }
                    } catch (Exception e) {
                        rowErrors.add("Invalid CGPA: '" + cgpaStr + "'");
                    }
                }

                Integer backlogs = 0;
                if (!backlogsStr.isBlank()) {
                    try {
                        backlogs = Integer.parseInt(backlogsStr);
                        if (backlogs < 0) {
                            rowErrors.add("Backlogs count cannot be negative");
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add("Invalid Backlogs count: '" + backlogsStr + "'");
                    }
                }

                if (!email.isBlank()) {
                    seenEmailsInFile.add(email);
                }

                if (!rowErrors.isEmpty()) {
                    failedCount++;
                    errors.add(RowErrorDto.builder()
                            .rowNumber(rowNum)
                            .email(email.isBlank() ? "N/A" : email)
                            .message(String.join("; ", rowErrors))
                            .build());
                    continue;
                }

                try {
                    String rawPass = (password != null && !password.isBlank()) ? password : "Student@123";
                    User user = User.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(rawPass))
                            .role(Role.STUDENT)
                            .build();
                    user = userRepository.save(user);

                    Set<Skill> skills = new HashSet<>();
                    if (!skillsStr.isBlank()) {
                        String[] tags = skillsStr.split("[|,]");
                        for (String t : tags) {
                            String clean = t.trim();
                            if (!clean.isBlank()) {
                                Skill skill = skillRepository.findByNameIgnoreCase(clean)
                                        .orElseGet(() -> skillRepository.save(Skill.builder().name(clean).build()));
                                skills.add(skill);
                            }
                        }
                    }

                    StudentProfile profile = StudentProfile.builder()
                            .user(user)
                            .fullName(name)
                            .phone(phone)
                            .branch(branch)
                            .graduationYear(year)
                            .cgpa(cgpa)
                            .backlogs(backlogs)
                            .skills(skills)
                            .build();
                    profile.recomputeCompleteness();
                    studentProfileRepository.save(profile);

                    successfulCount++;
                } catch (Exception ex) {
                    failedCount++;
                    errors.add(RowErrorDto.builder()
                            .rowNumber(rowNum)
                            .email(email)
                            .message("Database error: " + ex.getMessage())
                            .build());
                }
            }
        }

        return BulkImportResponse.builder()
                .totalRows(totalRows)
                .successfulCount(successfulCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    private int findHeaderIndex(List<String> headers, String... candidates) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).replaceAll("[_\\-\\s]", "");
            for (String c : candidates) {
                if (h.equalsIgnoreCase(c.replaceAll("[_\\-\\s]", ""))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getColValue(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) return "";
        return cols.get(index).trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
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
