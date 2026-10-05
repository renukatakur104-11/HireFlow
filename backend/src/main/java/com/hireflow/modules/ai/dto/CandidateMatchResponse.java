package com.hireflow.modules.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchResponse {
    private Long driveId;
    private String companyName;
    private String roleTitle;
    private int totalCandidates;
    private boolean aiAssisted;
    private String disclaimer;
    private List<CandidateMatchResult> candidates;
}
