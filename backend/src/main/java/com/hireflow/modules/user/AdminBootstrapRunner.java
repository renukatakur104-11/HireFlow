package com.hireflow.modules.user;

import com.hireflow.modules.student.Skill;
import com.hireflow.modules.student.SkillRepository;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hireflow.bootstrap.admin.email:admin@hireflow.edu}")
    private String adminEmail;

    @Value("${hireflow.bootstrap.admin.password:AdminSecurePassword123!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.ADMIN) == 0) {
            log.info("No admin account found. Bootstrapping initial admin: {}", adminEmail);

            User admin = User.builder()
                    .email(adminEmail.trim().toLowerCase())
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);

            log.info("Initial admin account created successfully: {}", adminEmail);
        } else {
            log.debug("Admin account already exists. Skipping bootstrap.");
        }

        // Seed demo student account if not present (dev convenience)
        String demoStudentEmail = "student@hireflow.edu";
        if (!userRepository.existsByEmail(demoStudentEmail)) {
            log.info("Seeding demo student account: {}", demoStudentEmail);
            User demoUser = User.builder()
                    .email(demoStudentEmail)
                    .passwordHash(passwordEncoder.encode("StudentPass123!"))
                    .role(Role.STUDENT)
                    .active(true)
                    .build();
            demoUser = userRepository.save(demoUser);

            Skill javaSkill = skillRepository.findByNameIgnoreCase("Java").orElse(null);
            Skill springSkill = skillRepository.findByNameIgnoreCase("Spring Boot").orElse(null);

            var skills = new HashSet<Skill>();
            if (javaSkill != null) skills.add(javaSkill);
            if (springSkill != null) skills.add(springSkill);

            StudentProfile demoProfile = StudentProfile.builder()
                    .user(demoUser)
                    .fullName("Demo Student")
                    .phone("9876543210")
                    .branch("Computer Science")
                    .graduationYear(2026)
                    .cgpa(BigDecimal.valueOf(8.5))
                    .backlogs(0)
                    .skills(skills)
                    .build();
            demoProfile.recomputeCompleteness();
            studentProfileRepository.save(demoProfile);
            log.info("Demo student account seeded (no resume – completeness={})", demoProfile.isProfileComplete());
        }
    }
}
