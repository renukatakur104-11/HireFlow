package com.hireflow.modules.application.dto;

import com.hireflow.modules.application.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusHistoryDto {
    private Long id;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private String changedByEmail;
    private String remarks;
    private boolean override;
    private LocalDateTime createdAt;
}
