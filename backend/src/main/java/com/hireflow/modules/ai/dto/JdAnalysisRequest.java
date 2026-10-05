package com.hireflow.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdAnalysisRequest {
    @NotBlank(message = "Job description cannot be empty")
    private String jobDescription;
}
