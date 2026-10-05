package com.hireflow.modules.notification.dto;

import com.hireflow.modules.notification.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    @NotNull
    private NotificationPriority priority;
    // Audience targeting
    private String audience; // ALL, DRIVE_ELIGIBLE, DRIVE_APPLICANTS, ROUND_APPLICANTS
    private Long driveId;
    private String roundStatus;
    private List<String> branches;
    private Integer year;
    private BigDecimal minCgpa;
    private List<String> skills;
}
