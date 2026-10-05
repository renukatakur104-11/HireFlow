package com.hireflow.modules.ai.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMatchScoreDto {
    private double score;
    private String explanation;
}
