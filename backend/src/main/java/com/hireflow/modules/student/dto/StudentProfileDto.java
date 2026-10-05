package com.hireflow.modules.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String branch;
    private Integer graduationYear;
    private BigDecimal cgpa;
    private Integer backlogs;
    private List<String> skills;
    private boolean profileComplete;
    private int completenessPercent;
    private boolean hasResume;
    private String resumeOriginalName;
    private LocalDateTime resumeUploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
