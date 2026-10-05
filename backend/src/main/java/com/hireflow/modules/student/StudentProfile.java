package com.hireflow.modules.student;

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
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String branch;

    @Column(name = "graduation_year", nullable = false)
    private Integer graduationYear;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Builder.Default
    @Column(nullable = false)
    private Integer backlogs = 0;

    @Column(name = "resume_file_path", length = 255)
    private String resumeFilePath;

    @Column(name = "resume_original_name", length = 255)
    private String resumeOriginalName;

    @Column(name = "resume_uploaded_at")
    private LocalDateTime resumeUploadedAt;

    @Builder.Default
    @Column(name = "is_profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_skills",
            joinColumns = @JoinColumn(name = "student_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean recomputeCompleteness() {
        boolean complete = fullName != null && !fullName.isBlank()
                && phone != null && !phone.isBlank()
                && branch != null && !branch.isBlank()
                && graduationYear != null && graduationYear >= 2020
                && cgpa != null && cgpa.compareTo(BigDecimal.ZERO) >= 0 && cgpa.compareTo(BigDecimal.valueOf(10.0)) <= 0
                && skills != null && !skills.isEmpty()
                && resumeFilePath != null && !resumeFilePath.isBlank();
        this.profileComplete = complete;
        return complete;
    }
}
