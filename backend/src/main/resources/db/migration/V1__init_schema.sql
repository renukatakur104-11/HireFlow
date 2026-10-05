-- ====================================================================
-- Flyway Migration V1: Core Schema for HireFlow Platform
-- Target Database: Aiven Cloud MySQL 8.0
-- ====================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. STUDENT PROFILES TABLE
CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    branch VARCHAR(50) NOT NULL,
    graduation_year INT NOT NULL,
    cgpa DECIMAL(4, 2) NOT NULL,
    backlogs INT NOT NULL DEFAULT 0,
    resume_file_path VARCHAR(255) NULL,
    resume_original_name VARCHAR(255) NULL,
    resume_uploaded_at DATETIME NULL,
    is_profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_student_branch_cgpa (branch, cgpa),
    INDEX idx_student_grad_year (graduation_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. SKILLS MASTER TABLE
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    INDEX idx_skills_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. STUDENT SKILLS (MANY-TO-MANY)
CREATE TABLE IF NOT EXISTS student_skills (
    student_profile_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (student_profile_id, skill_id),
    CONSTRAINT fk_student_skills_profile FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. DRIVES TABLE
CREATE TABLE IF NOT EXISTS drives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    role_title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    package_lpa DECIMAL(6, 2) NOT NULL,
    package_details VARCHAR(255) NULL,
    location VARCHAR(150) NOT NULL,
    min_cgpa DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    max_backlogs INT NOT NULL DEFAULT 0,
    drive_date DATETIME NOT NULL,
    application_deadline DATETIME NOT NULL,
    status ENUM('DRAFT', 'OPEN', 'CLOSED', 'COMPLETED') NOT NULL DEFAULT 'DRAFT',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_drives_creator FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_drives_status_deadline (status, application_deadline, is_deleted),
    INDEX idx_drives_date (drive_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. DRIVE BRANCHES (ELIGIBILITY MAPPING)
CREATE TABLE IF NOT EXISTS drive_branches (
    drive_id BIGINT NOT NULL,
    branch_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (drive_id, branch_name),
    CONSTRAINT fk_drive_branches_drive FOREIGN KEY (drive_id) REFERENCES drives(id) ON DELETE CASCADE,
    INDEX idx_drive_branches_branch (branch_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. DRIVE SKILLS (REQUIRED SKILLS MAPPING)
CREATE TABLE IF NOT EXISTS drive_skills (
    drive_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (drive_id, skill_id),
    CONSTRAINT fk_drive_skills_drive FOREIGN KEY (drive_id) REFERENCES drives(id) ON DELETE CASCADE,
    CONSTRAINT fk_drive_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. DRIVE ROUNDS CONFIGURATION
CREATE TABLE IF NOT EXISTS drive_rounds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id BIGINT NOT NULL,
    round_order INT NOT NULL,
    round_name VARCHAR(100) NOT NULL,
    status_code ENUM('ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW') NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT fk_drive_rounds_drive FOREIGN KEY (drive_id) REFERENCES drives(id) ON DELETE CASCADE,
    UNIQUE KEY uk_drive_round_order (drive_id, round_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. APPLICATIONS TABLE
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id BIGINT NOT NULL,
    student_profile_id BIGINT NOT NULL,
    current_status ENUM('REGISTERED', 'ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'REGISTERED',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_drive FOREIGN KEY (drive_id) REFERENCES drives(id),
    CONSTRAINT fk_app_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_drive_student (drive_id, student_profile_id),
    INDEX idx_applications_student_status (student_profile_id, current_status),
    INDEX idx_applications_drive_status (drive_id, current_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. APPLICATION STATUS HISTORY
CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    from_status ENUM('REGISTERED', 'ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN') NULL,
    to_status ENUM('REGISTERED', 'ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN') NOT NULL,
    changed_by BIGINT NOT NULL,
    remarks TEXT NULL,
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_hist_user FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_hist_app_created (application_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. NOTIFICATIONS MASTER
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    priority ENUM('NORMAL', 'URGENT') NOT NULL DEFAULT 'NORMAL',
    type ENUM('DRIVE_PUBLISHED', 'DRIVE_UPDATED', 'DEADLINE_APPROACHING', 'STATUS_CHANGED', 'APPLICATION_CONFIRMED', 'DRIVE_REMINDER', 'CUSTOM_BROADCAST') NOT NULL,
    target_drive_id BIGINT NULL,
    created_by BIGINT NULL,
    dedup_key VARCHAR(255) NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_drive FOREIGN KEY (target_drive_id) REFERENCES drives(id) ON DELETE SET NULL,
    CONSTRAINT fk_notif_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notif_type_created (type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. NOTIFICATION RECIPIENTS
CREATE TABLE IF NOT EXISTS notification_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recip_notif FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_recip_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_notif_user (notification_id, user_id),
    INDEX idx_recip_user_read (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. REFRESH TOKENS (SESSION STORE & ROTATION)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_hash VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tokens_user_revoked (user_id, is_revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. AUDIT LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    details_json TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_action_created (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
