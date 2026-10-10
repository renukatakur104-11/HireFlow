package com.hireflow.modules.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Writes audit records for the 5 most important admin actions.
 *
 * Design principles:
 * - Runs in a REQUIRES_NEW transaction so a business-logic rollback does NOT
 *   lose the audit entry (and an audit failure does NOT roll back the business tx).
 * - All writes are fire-and-forget: callers catch exceptions and log a warning.
 * - No credentials, passwords, or PII beyond email are persisted in details_json.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // ── Action constants – used as the `action` column value ──────────────────

    public static final String DRIVE_CREATED          = "DRIVE_CREATED";
    public static final String DRIVE_DELETED          = "DRIVE_DELETED";
    public static final String APPLICATION_STATUS_CHANGED = "APPLICATION_STATUS_CHANGED";
    public static final String STUDENT_BULK_IMPORTED  = "STUDENT_BULK_IMPORTED";
    public static final String NOTIFICATION_BROADCAST = "NOTIFICATION_BROADCAST";

    // ── Public write methods ───────────────────────────────────────────────────

    /** Called when an admin creates a placement drive */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDriveCreated(Long adminUserId, Long driveId, String companyName, String roleTitle, String ip) {
        write(adminUserId, DRIVE_CREATED, "Drive", driveId,
                Map.of("companyName", companyName, "roleTitle", roleTitle), ip);
    }

    /** Called when an admin soft-deletes a drive */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDriveDeleted(Long adminUserId, Long driveId, String companyName, String ip) {
        write(adminUserId, DRIVE_DELETED, "Drive", driveId,
                Map.of("companyName", companyName), ip);
    }

    /** Called when an admin changes a single application's status */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApplicationStatusChanged(Long adminUserId, Long applicationId,
                                            String fromStatus, String toStatus,
                                            boolean override, String ip) {
        write(adminUserId, APPLICATION_STATUS_CHANGED, "Application", applicationId,
                Map.of("fromStatus", fromStatus != null ? fromStatus : "N/A",
                       "toStatus", toStatus,
                       "override", override), ip);
    }

    /** Called after a bulk CSV student import completes */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStudentBulkImported(Long adminUserId, int totalRows,
                                       int successfulCount, int failedCount, String ip) {
        write(adminUserId, STUDENT_BULK_IMPORTED, "StudentProfile", null,
                Map.of("totalRows", totalRows,
                       "successfulCount", successfulCount,
                       "failedCount", failedCount), ip);
    }

    /** Called when an admin sends a broadcast notification */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNotificationBroadcast(Long adminUserId, String audience,
                                         int recipientCount, String ip) {
        write(adminUserId, NOTIFICATION_BROADCAST, "Notification", null,
                Map.of("audience", audience, "recipientCount", recipientCount), ip);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void write(Long userId, String action, String entityType,
                       Long entityId, Map<String, Object> details, String ip) {
        try {
            String json = objectMapper.writeValueAsString(details);
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .detailsJson(json)
                    .ipAddress(ip)
                    .build();
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            // Should never happen with a simple Map<String, Object>
            log.warn("AuditService: failed to serialize details for action {}: {}", action, e.getMessage());
        } catch (Exception e) {
            log.warn("AuditService: failed to persist audit log for action {}: {}", action, e.getMessage());
        }
    }
}
