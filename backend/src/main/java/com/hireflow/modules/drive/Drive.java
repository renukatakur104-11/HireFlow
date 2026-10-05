package com.hireflow.modules.drive;

import com.hireflow.modules.student.Skill;
import com.hireflow.modules.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "drives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "role_title", nullable = false, length = 150)
    private String roleTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "package_lpa", nullable = false, precision = 6, scale = 2)
    private BigDecimal packageLpa;

    @Column(name = "package_details", length = 255)
    private String packageDetails;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(name = "min_cgpa", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal minCgpa = BigDecimal.ZERO;

    @Column(name = "max_backlogs", nullable = false)
    @Builder.Default
    private Integer maxBacklogs = 0;

    @Column(name = "drive_date", nullable = false)
    private LocalDateTime driveDate;

    @Column(name = "application_deadline", nullable = false)
    private LocalDateTime applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DriveStatus status = DriveStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Allowed branches stored as element collection for simplicity
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "drive_branches", joinColumns = @JoinColumn(name = "drive_id"))
    @Column(name = "branch_name", length = 50)
    @Builder.Default
    private Set<String> allowedBranches = new HashSet<>();

    // Required skills (many-to-many via drive_skills table)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "drive_skills",
            joinColumns = @JoinColumn(name = "drive_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> requiredSkills = new HashSet<>();

    @OneToMany(mappedBy = "drive", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundOrder ASC")
    @Builder.Default
    private Set<DriveRound> rounds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
