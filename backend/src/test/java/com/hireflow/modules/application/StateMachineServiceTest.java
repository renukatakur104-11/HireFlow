package com.hireflow.modules.application;

import com.hireflow.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineServiceTest {

    private StateMachineService stateMachineService;

    @BeforeEach
    void setUp() {
        stateMachineService = new StateMachineService();
    }

    @Test
    @DisplayName("Valid transition from REGISTERED to ROUND_1 succeeds")
    void testValidTransition() {
        assertDoesNotThrow(() ->
                stateMachineService.validateTransition(ApplicationStatus.REGISTERED, ApplicationStatus.ROUND_1, false, null)
        );
    }

    @Test
    @DisplayName("Transitioning to the same status throws BadRequestException")
    void testSameStatusThrows() {
        assertThrows(BadRequestException.class, () ->
                stateMachineService.validateTransition(ApplicationStatus.ROUND_1, ApplicationStatus.ROUND_1, false, null)
        );
    }

    @Test
    @DisplayName("Transitioning from terminal state SELECTED without override throws BadRequestException")
    void testTerminalStateRequiresOverride() {
        assertThrows(BadRequestException.class, () ->
                stateMachineService.validateTransition(ApplicationStatus.SELECTED, ApplicationStatus.ROUND_2, false, null)
        );
    }

    @Test
    @DisplayName("Override transition without remarks throws BadRequestException")
    void testOverrideRequiresRemarks() {
        assertThrows(BadRequestException.class, () ->
                stateMachineService.validateTransition(ApplicationStatus.REJECTED, ApplicationStatus.ROUND_1, true, "   ")
        );
    }

    @Test
    @DisplayName("Override transition with valid remarks succeeds")
    void testOverrideWithRemarksSucceeds() {
        assertDoesNotThrow(() ->
                stateMachineService.validateTransition(ApplicationStatus.REJECTED, ApplicationStatus.ROUND_1, true, "Candidate appeal accepted")
        );
    }
}
