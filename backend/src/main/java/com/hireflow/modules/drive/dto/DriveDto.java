package com.hireflow.modules.drive.dto;

import com.hireflow.modules.drive.DriveStatus;
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
public class DriveDto {
    private Long id;
    private String companyName;
    private String roleTitle;
    private String description;
    private BigDecimal packageLpa;
    private String packageDetails;
    private String location;
    private BigDecimal minCgpa;
    private Integer maxBacklogs;
    private LocalDateTime driveDate;
    private LocalDateTime applicationDeadline;
    private DriveStatus status;
    private List<String> allowedBranches;
    private List<String> requiredSkills;
    private List<DriveRoundDto> rounds;
    private int totalApplications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Student-specific fields (populated when queried by student)
    private Boolean isEligible;
    private Boolean hasApplied;
    private List<String> eligibilityReasons;
}
