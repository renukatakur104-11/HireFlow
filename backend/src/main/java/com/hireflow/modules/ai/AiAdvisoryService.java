package com.hireflow.modules.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.modules.ai.dto.*;
import com.hireflow.modules.application.Application;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.drive.Drive;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.modules.student.StudentProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAdvisoryService {

    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    @Value("${hireflow.ai.provider:openai}")
    private String aiProvider;

    @Value("${hireflow.ai.api-key:}")
    private String aiApiKey;

    @Value("${hireflow.ai.model:gpt-4o-mini}")
    private String aiModel;

    @Value("${hireflow.ai.base-url:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${hireflow.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * Analyze a job description and extract structured eligibility requirements.
     * If AI is disabled or fails, returns a rule-based extraction.
     */
    public JdAnalysisResponse analyzeJobDescription(JdAnalysisRequest request) {
        if (!aiEnabled || aiApiKey.isBlank()) {
            log.info("AI disabled or no API key — using rule-based JD analysis");
            return ruleBasedJdAnalysis(request.getJobDescription());
        }

        try {
            String prompt = buildJdAnalysisPrompt(request.getJobDescription());
            String rawResponse = callAiApi(prompt);
            return parseJdAnalysisResponse(rawResponse, request.getJobDescription());
        } catch (Exception e) {
            log.warn("AI JD analysis failed: {} — falling back to rule-based", e.getMessage());
            return ruleBasedJdAnalysis(request.getJobDescription());
        }
    }

    /**
     * Match eligible candidates against drive criteria using AI assistance.
     * Eligibility has ALREADY been determined deterministically; AI only adds a match score and explanation.
     */
    public CandidateMatchResponse matchCandidates(CandidateMatchRequest request) {
        Drive drive = driveRepository.findByIdAndDeletedFalse(request.getDriveId())
                .orElseThrow(() -> new ResourceNotFoundException("Drive", "id", request.getDriveId()));

        // Fetch eligible applicants (already passed deterministic eligibility checks)
        List<Application> applications = applicationRepository.findByDriveId(drive.getId(), Pageable.unpaged()).getContent();

        List<CandidateMatchResult> results = new ArrayList<>();

        for (Application app : applications) {
            StudentProfile student = app.getStudentProfile();
            CandidateMatchResult result = buildBasicMatchResult(app, student, drive);

            if (aiEnabled && !aiApiKey.isBlank() && request.getJobDescription() != null) {
                try {
                    result = enrichWithAiScore(result, student, drive, request.getJobDescription());
                } catch (Exception e) {
                    log.warn("AI enrichment failed for student {}: {}", student.getId(), e.getMessage());
                    // Keep basic result without AI enrichment
                }
            }
            results.add(result);
        }

        // Sort by AI score (desc), then by CGPA (desc)
        results.sort(Comparator.comparingDouble(CandidateMatchResult::getMatchScore).reversed()
                .thenComparingDouble(r -> -r.getCgpa()));

        return CandidateMatchResponse.builder()
                .driveId(drive.getId())
                .companyName(drive.getCompanyName())
                .roleTitle(drive.getRoleTitle())
                .totalCandidates(results.size())
                .aiAssisted(aiEnabled && !aiApiKey.isBlank())
                .disclaimer("AI scores are advisory only. The Placement Coordinator makes all final decisions.")
                .candidates(results)
                .build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private CandidateMatchResult buildBasicMatchResult(Application app, StudentProfile student, Drive drive) {
        Set<String> studentSkillNames = student.getSkills().stream()
                .map(s -> s.getName().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> requiredSkillNames = drive.getRequiredSkills().stream()
                .map(s -> s.getName().toLowerCase())
                .collect(Collectors.toSet());
        long matchedSkills = requiredSkillNames.stream().filter(studentSkillNames::contains).count();
        double skillScore = requiredSkillNames.isEmpty() ? 1.0 : (double) matchedSkills / requiredSkillNames.size();

        // Basic deterministic score: 50% CGPA normalized + 50% skill match
        double cgpaMax = drive.getMinCgpa() != null ? Math.max(drive.getMinCgpa().doubleValue(), 10.0) : 10.0;
        double cgpaScore = student.getCgpa() != null ? student.getCgpa().doubleValue() / cgpaMax : 0;
        double basicScore = Math.min(1.0, (cgpaScore * 0.5) + (skillScore * 0.5));

        return CandidateMatchResult.builder()
                .applicationId(app.getId())
                .studentProfileId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getUser().getEmail())
                .branch(student.getBranch())
                .cgpa(student.getCgpa() != null ? student.getCgpa().doubleValue() : 0)
                .skills(student.getSkills().stream().map(s -> s.getName()).collect(Collectors.toList()))
                .currentStatus(app.getCurrentStatus().name())
                .matchScore(Math.round(basicScore * 100.0) / 100.0)
                .matchExplanation("Deterministic score based on CGPA and skill match.")
                .aiEnriched(false)
                .build();
    }

    private CandidateMatchResult enrichWithAiScore(CandidateMatchResult base, StudentProfile student, Drive drive, String jd) {
        String prompt = buildCandidateMatchPrompt(student, drive, jd);
        String rawResponse = callAiApi(prompt);

        try {
            // Try to extract JSON from AI response
            AiMatchScoreDto aiScore = parseAiMatchScore(rawResponse);
            return base.toBuilder()
                    .matchScore(aiScore.getScore())
                    .matchExplanation(aiScore.getExplanation())
                    .aiEnriched(true)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse AI match score: {}", e.getMessage());
            return base;
        }
    }

    private String callAiApi(String prompt) {
        // Use a RestTemplate with explicit connect/read timeouts to prevent thread starvation
        // when the AI provider is slow or unreachable. The caller catches any exception and
        // falls back to rule-based analysis, so this timeout is purely a safety net.
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);  // 5s to establish TCP connection
        factory.setReadTimeout(15_000);    // 15s to read the full AI response
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", aiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an expert HR assistant that analyzes job descriptions and candidate profiles. Always respond with valid JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 800
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + "/chat/completions", entity, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String buildJdAnalysisPrompt(String jobDescription) {
        return """
                Analyze the following job description and extract structured information.
                Respond ONLY with valid JSON in this exact format:
                {
                  "extractedRole": "extracted role title",
                  "extractedSkills": ["skill1", "skill2"],
                  "suggestedMinCgpa": 7.0,
                  "suggestedBranches": ["Computer Science", "Information Technology"],
                  "summary": "2-3 sentence summary of the role",
                  "keyRequirements": ["requirement1", "requirement2"]
                }
                
                Job Description:
                """ + jobDescription;
    }

    private String buildCandidateMatchPrompt(StudentProfile student, Drive drive, String jd) {
        String skills = student.getSkills().stream().map(s -> s.getName()).collect(Collectors.joining(", "));
        String reqSkills = drive.getRequiredSkills().stream().map(s -> s.getName()).collect(Collectors.joining(", "));
        return String.format("""
                Rate this candidate for the job position. Respond ONLY with valid JSON:
                {"score": 0.85, "explanation": "Brief explanation max 2 sentences"}
                Score must be between 0.0 and 1.0.
                
                Candidate: %s | Branch: %s | CGPA: %s | Skills: %s
                Job: %s at %s | Required Skills: %s
                Job Description (excerpt): %s
                """,
                student.getFullName(), student.getBranch(),
                student.getCgpa(), skills,
                drive.getRoleTitle(), drive.getCompanyName(), reqSkills,
                jd.substring(0, Math.min(500, jd.length())));
    }

    private JdAnalysisResponse parseJdAnalysisResponse(String rawResponse, String originalJd) {
        try {
            // Find JSON in the response
            int start = rawResponse.indexOf('{');
            int end = rawResponse.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                String json = rawResponse.substring(start, end);
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
                @SuppressWarnings("unchecked")
                List<String> skills = (List<String>) parsed.getOrDefault("extractedSkills", List.of());
                @SuppressWarnings("unchecked")
                List<String> branches = (List<String>) parsed.getOrDefault("suggestedBranches", List.of());
                @SuppressWarnings("unchecked")
                List<String> requirements = (List<String>) parsed.getOrDefault("keyRequirements", List.of());
                return JdAnalysisResponse.builder()
                        .extractedRole((String) parsed.getOrDefault("extractedRole", ""))
                        .extractedSkills(skills)
                        .suggestedMinCgpa(parsed.get("suggestedMinCgpa") instanceof Number n ? n.doubleValue() : 0.0)
                        .suggestedBranches(branches)
                        .summary((String) parsed.getOrDefault("summary", ""))
                        .keyRequirements(requirements)
                        .aiGenerated(true)
                        .disclaimer("AI-generated analysis. Please verify before using.")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI JD response: {}", e.getMessage());
        }
        return ruleBasedJdAnalysis(originalJd);
    }

    private AiMatchScoreDto parseAiMatchScore(String rawResponse) throws Exception {
        int start = rawResponse.indexOf('{');
        int end = rawResponse.lastIndexOf('}') + 1;
        if (start >= 0 && end > start) {
            String json = rawResponse.substring(start, end);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            double score = parsed.get("score") instanceof Number n ? n.doubleValue() : 0.5;
            score = Math.max(0.0, Math.min(1.0, score)); // clamp to [0,1]
            return new AiMatchScoreDto(score, (String) parsed.getOrDefault("explanation", ""));
        }
        throw new IllegalArgumentException("No JSON found in AI response");
    }

    private JdAnalysisResponse ruleBasedJdAnalysis(String jd) {
        String lower = jd.toLowerCase();
        List<String> skills = new ArrayList<>();
        List<String> knownSkills = List.of("java", "python", "javascript", "typescript", "react", "angular",
                "spring", "sql", "mysql", "mongodb", "docker", "kubernetes", "aws", "git",
                "machine learning", "data science", "c++", "node.js", "rest api");
        for (String skill : knownSkills) {
            if (lower.contains(skill)) skills.add(skill);
        }

        List<String> branches = new ArrayList<>();
        if (lower.contains("computer science") || lower.contains("cse")) branches.add("Computer Science");
        if (lower.contains("information technology") || lower.contains("it")) branches.add("Information Technology");
        if (lower.contains("electronics") || lower.contains("ece")) branches.add("Electronics & Communication");

        return JdAnalysisResponse.builder()
                .extractedRole("")
                .extractedSkills(skills)
                .suggestedMinCgpa(6.0)
                .suggestedBranches(branches.isEmpty() ? List.of("Computer Science", "Information Technology") : branches)
                .summary("Rule-based analysis of the provided job description.")
                .keyRequirements(List.of())
                .aiGenerated(false)
                .disclaimer("Rule-based analysis (AI is disabled or unavailable). Results may be approximate.")
                .build();
    }
}
