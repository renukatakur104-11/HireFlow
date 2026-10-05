package com.hireflow.modules.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchResult {
    private Long applicationId;
    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private String branch;
    private double cgpa;
    private List<String> skills;
    private String currentStatus;
    private double matchScore;
    private String matchExplanation;
    private boolean aiEnriched;
}
