package com.hireflow.modules.ai;

import com.hireflow.common.response.ApiResponse;
import com.hireflow.modules.ai.dto.CandidateMatchRequest;
import com.hireflow.modules.ai.dto.CandidateMatchResponse;
import com.hireflow.modules.ai.dto.JdAnalysisRequest;
import com.hireflow.modules.ai.dto.JdAnalysisResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AiController {

    private final AiAdvisoryService aiAdvisoryService;

    @PostMapping("/analyze-jd")
    public ResponseEntity<ApiResponse<JdAnalysisResponse>> analyzeJd(@Valid @RequestBody JdAnalysisRequest request) {
        JdAnalysisResponse response = aiAdvisoryService.analyzeJobDescription(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Job description analyzed successfully"));
    }

    @PostMapping("/match-candidates")
    public ResponseEntity<ApiResponse<CandidateMatchResponse>> matchCandidates(@Valid @RequestBody CandidateMatchRequest request) {
        CandidateMatchResponse response = aiAdvisoryService.matchCandidates(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate matching completed"));
    }
}
