package com.hireflow.modules.drive;

import java.util.List;

/**
 * Authoritative eligibility result. This is the single source of truth.
 * Lives in one service and is reused by API, notifications, and applicant validation.
 */
public record EligibilityResult(
        boolean eligible,
        List<String> reasons   // reasons why ineligible (empty if eligible)
) {
    /** Factory: creates an eligible result */
    public static EligibilityResult pass() {
        return new EligibilityResult(true, List.of());
    }

    /** Factory: creates an ineligible result with reasons */
    public static EligibilityResult fail(List<String> reasons) {
        return new EligibilityResult(false, reasons);
    }
}
