package com.hireflow.modules.application.dto;

import com.hireflow.modules.application.ApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusUpdateRequest {
    @NotEmpty
    private List<Long> applicationIds;
    @NotNull
    private ApplicationStatus newStatus;
    private String remarks;
    private boolean override;
}
