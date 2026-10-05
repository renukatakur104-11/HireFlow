package com.hireflow.modules.drive.dto;

import com.hireflow.modules.drive.DriveStatus;
import jakarta.validation.constraints.*;
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
public class DriveCreateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150)
    private String companyName;

    @NotBlank(message = "Role title is required")
    @Size(max = 150)
    private String roleTitle;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Package is required")
    @DecimalMin(value = "0.00")
    private BigDecimal packageLpa;

    private String packageDetails;

    @NotBlank(message = "Location is required")
    private String location;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "10.00")
    @Builder.Default
    private BigDecimal minCgpa = BigDecimal.ZERO;

    @Min(0)
    @Builder.Default
    private Integer maxBacklogs = 0;

    @NotNull(message = "Drive date is required")
    private LocalDateTime driveDate;

    @NotNull(message = "Application deadline is required")
    private LocalDateTime applicationDeadline;

    private DriveStatus status;

    private List<String> allowedBranches;

    private List<String> requiredSkills;

    private List<DriveRoundRequest> rounds;
}
