package com.hireflow.modules.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdAnalysisResponse {
    private String extractedRole;
    private List<String> extractedSkills;
    private Double suggestedMinCgpa;
    private List<String> suggestedBranches;
    private String summary;
    private List<String> keyRequirements;
    private boolean aiGenerated;
    private String disclaimer;
}
