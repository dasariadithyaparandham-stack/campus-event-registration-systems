-- ===================================================================
-- Campus Event Registration System - Flyway Migration V3: Advanced Features
-- (Waitlist, Certificates, Post-Event Feedback & Analytics Indexes)
-- ===================================================================

-- 1. Waitlist Table
CREATE TABLE IF NOT EXISTS waitlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    queue_position INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    offered_at DATETIME NULL,
    offer_expires_at DATETIME NULL,
    responded_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_waitlist_key VARCHAR(100) GENERATED ALWAYS AS (
        CASE WHEN status IN ('WAITING', 'OFFERED') THEN CONCAT(student_id, '-', event_id) ELSE NULL END
    ) STORED,
    CONSTRAINT uq_active_waitlist UNIQUE (active_waitlist_key),
    CONSTRAINT fk_waitlist_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_waitlist_student FOREIGN KEY (student_id) REFERENCES users(id),
    INDEX idx_waitlist_event_status (event_id, status),
    INDEX idx_waitlist_student (student_id),
    INDEX idx_waitlist_queue (event_id, queue_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Certificates Table
CREATE TABLE IF NOT EXISTS certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_code VARCHAR(64) NOT NULL UNIQUE,
    registration_id BIGINT NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    student_name VARCHAR(150) NOT NULL,
    event_title VARCHAR(200) NOT NULL,
    event_date DATETIME NOT NULL,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cert_registration FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_cert_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_cert_event FOREIGN KEY (event_id) REFERENCES events(id),
    INDEX idx_cert_code (certificate_code),
    INDEX idx_cert_student (student_id),
    INDEX idx_cert_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Post-Event Feedback & Ratings Table
CREATE TABLE IF NOT EXISTS event_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL UNIQUE,
    rating INT NOT NULL,
    comments TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_event_feedback UNIQUE (student_id, event_id),
    CONSTRAINT fk_feedback_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_feedback_registration FOREIGN KEY (registration_id) REFERENCES registrations(id),
    INDEX idx_feedback_event (event_id),
    INDEX idx_feedback_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Sample Organizer User for Demonstration
INSERT INTO users (id, full_name, email, password_hash, role, student_id_number, phone, department, active, created_at)
VALUES 
(10, 'Prof. Sarah Jenkins', 'organizer@campus.edu', '$2a$10$7ZkQ6zNq2z0VvB1zJc0yRe0wKq.LgZ0cE2Kq7K4B5eT6N8Q1yO0qS', 'ORGANIZER', 'ORG-FAC-01', '+91 9876543299', 'Computer Science & Engineering', 1, NOW())
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 5. Assign Organizer to Event 2 (Hands-on Generative AI Workshop)
UPDATE events SET organizer_id = 10 WHERE id = 2;

