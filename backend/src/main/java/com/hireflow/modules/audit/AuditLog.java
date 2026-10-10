package com.hireflow.modules.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persists key administrative actions into the existing audit_logs table
 * defined in V1__init_schema.sql. Only admin-initiated mutations are logged.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable – null when the action was performed by an unauthenticated system task */
    @Column(name = "user_id")
    private Long userId;

    /** Short action descriptor, e.g. DRIVE_CREATED, STUDENT_BULK_IMPORTED */
    @Column(nullable = false, length = 100)
    private String action;

    /** Entity type that was mutated, e.g. Drive, Application, StudentProfile */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** Primary key of the affected entity row (nullable for bulk/global actions) */
    @Column(name = "entity_id")
    private Long entityId;

    /** Optional JSON payload with additional context (not credentials) */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    /** Remote address of the requester when available */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
