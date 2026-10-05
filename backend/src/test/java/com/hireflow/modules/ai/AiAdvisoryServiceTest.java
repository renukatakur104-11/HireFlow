package com.hireflow.modules.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.modules.ai.dto.JdAnalysisRequest;
import com.hireflow.modules.ai.dto.JdAnalysisResponse;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.drive.DriveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AiAdvisoryServiceTest {

    @Mock
    private DriveRepository driveRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiAdvisoryService aiAdvisoryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAdvisoryService, "aiEnabled", false);
        ReflectionTestUtils.setField(aiAdvisoryService, "aiApiKey", "");
    }

    @Test
    @DisplayName("When AI is disabled or key is empty, rule-based fallback extracts skills and branches")
    void testRuleBasedFallbackExtractsSkills() {
        String rawJd = "We are hiring Software Development Engineers in Computer Science. Must know Java, React, SQL, and Docker. Minimum CGPA 7.0.";

        JdAnalysisResponse response = aiAdvisoryService.analyzeJobDescription(new JdAnalysisRequest(rawJd));

        assertNotNull(response);
        assertFalse(response.isAiGenerated());
        assertNotNull(response.getExtractedSkills());
        assertTrue(response.getExtractedSkills().contains("java"));
        assertTrue(response.getExtractedSkills().contains("react"));
        assertTrue(response.getExtractedSkills().contains("docker"));
        assertTrue(response.getSuggestedBranches().contains("Computer Science"));
        assertNotNull(response.getDisclaimer());
    }
}
