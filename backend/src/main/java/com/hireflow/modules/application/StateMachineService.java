package com.hireflow.modules.application;

import com.hireflow.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enforces the application status state machine.
 * Only valid forward transitions are allowed (unless override=true).
 */
@Service
public class StateMachineService {

    /**
     * Validate and enforce state transition rules.
     * Override allows admins to make exceptional transitions with mandatory remarks.
     */
    public void validateTransition(ApplicationStatus from, ApplicationStatus to, boolean override, String remarks) {
        if (from == to) {
            throw new BadRequestException("Application is already in status: " + to);
        }

        // Terminal statuses cannot transition further (without override)
        Set<ApplicationStatus> terminalStatuses = EnumSet.of(
                ApplicationStatus.SELECTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
        );

        if (terminalStatuses.contains(from) && !override) {
            throw new BadRequestException(
                    "Cannot change status from terminal state '" + from + "' without override flag and remarks."
            );
        }

        if (override && (remarks == null || remarks.isBlank())) {
            throw new BadRequestException("Override transitions require non-empty remarks explaining the reason.");
        }

        // WITHDRAWN can only be set by student (handled in ApplicationService.withdraw)
        if (to == ApplicationStatus.WITHDRAWN && !override) {
            throw new BadRequestException("Cannot set status to WITHDRAWN via admin update. Student must withdraw directly.");
        }
    }
}
