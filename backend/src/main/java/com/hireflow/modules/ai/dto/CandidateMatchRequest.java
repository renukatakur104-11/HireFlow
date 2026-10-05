package com.hireflow.modules.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchRequest {
    @NotNull(message = "Drive ID is required")
    private Long driveId;
    private String jobDescription;
}
