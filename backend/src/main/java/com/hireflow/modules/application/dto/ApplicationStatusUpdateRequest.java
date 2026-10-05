package com.hireflow.modules.application.dto;

import com.hireflow.modules.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdateRequest {
    @NotNull(message = "New status is required")
    private ApplicationStatus newStatus;
    private String remarks;
    private boolean override;
}
