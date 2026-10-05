package com.hireflow.modules.application.dto;

import com.hireflow.modules.application.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long driveId;
    private String companyName;
    private String roleTitle;
    private String location;
    private java.math.BigDecimal packageLpa;
    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private String branch;
    private java.math.BigDecimal cgpa;
    private ApplicationStatus currentStatus;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private java.time.LocalDateTime driveDate;
    private java.time.LocalDateTime applicationDeadline;
}
