-- ==============================================================================
-- CampusFix Backend — Complete Database Schema (MySQL 8.0+)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_user_email UNIQUE (email),
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(64) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(50) NOT NULL,
    building VARCHAR(100) NOT NULL,
    floor VARCHAR(50) NOT NULL,
    room VARCHAR(50) NOT NULL,
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reporter_id BIGINT NOT NULL,
    assigned_staff_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    resolution_notes VARCHAR(2000) NULL,
    version BIGINT NULL,
    verification_status VARCHAR(30) NULL,
    verified_at DATETIME NULL,
    verified_by_id BIGINT NULL,
    verification_comment VARCHAR(2000) NULL,
    after_image_storage_key VARCHAR(100) NULL,
    CONSTRAINT uq_complaint_reference UNIQUE (reference_number),
    CONSTRAINT fk_complaint_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_complaint_assigned_staff FOREIGN KEY (assigned_staff_id) REFERENCES users(id),
    CONSTRAINT fk_complaint_verified_by FOREIGN KEY (verified_by_id) REFERENCES users(id),
    INDEX idx_complaint_reference (reference_number),
    INDEX idx_complaint_status (status),
    INDEX idx_complaint_category (category),
    INDEX idx_complaint_priority (priority),
    INDEX idx_complaint_reporter (reporter_id),
    INDEX idx_complaint_assigned_staff (assigned_staff_id),
    INDEX idx_complaint_created_at (created_at),
    INDEX idx_complaint_verification_status (verification_status),
    INDEX idx_complaint_location (building, floor, room)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    cycle_number INT NOT NULL,
    resolved_at DATETIME NOT NULL,
    resolution_notes VARCHAR(2000) NOT NULL,
    after_image_storage_key VARCHAR(100) NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_by_id BIGINT NULL,
    verified_at DATETIME NULL,
    verification_comment VARCHAR(2000) NULL,
    is_overridden_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    last_reminder_sent_at DATETIME NULL,
    reminder_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_verification_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_verified_by FOREIGN KEY (verified_by_id) REFERENCES users(id),
    INDEX idx_verification_complaint (complaint_id),
    INDEX idx_verification_status (verification_status),
    INDEX idx_verification_cycle (complaint_id, cycle_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(100) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    description VARCHAR(2000) NULL,
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_history_complaint (complaint_id),
    INDEX idx_history_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    CONSTRAINT uq_image_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_image_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    INDEX idx_image_complaint (complaint_id),
    INDEX idx_image_storage_key (storage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_token (token),
    INDEX idx_refresh_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    reference_number VARCHAR(64) NULL,
    event_type VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notification_recipient (recipient_id),
    INDEX idx_notification_unread (recipient_id, is_read),
    INDEX idx_notification_complaint (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    staff_id BIGINT NULL,
    rating INT NOT NULL,
    comment VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_feedback_complaint UNIQUE (complaint_id),
    CONSTRAINT fk_feedback_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_staff FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_feedback_complaint (complaint_id),
    INDEX idx_feedback_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
