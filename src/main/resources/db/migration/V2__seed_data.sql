-- ===================================================================
-- Campus Event Registration System - Flyway Migration V2: Seed Data
-- ===================================================================

-- Initial Admin and Sample Students
-- Default Admin: admin@campus.edu / Admin@123
-- Default Student: john.doe@campus.edu / Student@123
-- Default Student: jane.smith@campus.edu / Student@123
-- Default Student: raj.patel@campus.edu / Student@123

INSERT INTO users (id, full_name, email, password_hash, role, student_id_number, phone, department, active, created_at)
VALUES 
(1, 'Administrator', 'admin@campus.edu', '$2a$10$7ZkQ6zNq2z0VvB1zJc0yRe0wKq.LgZ0cE2Kq7K4B5eT6N8Q1yO0qS', 'ADMIN', 'ADM-001', '+91 9876543210', 'Administration', 1, NOW()),
(2, 'John Doe', 'john.doe@campus.edu', '$2a$10$7ZkQ6zNq2z0VvB1zJc0yRe0wKq.LgZ0cE2Kq7K4B5eT6N8Q1yO0qS', 'STUDENT', 'STU-2024-001', '+91 9876543211', 'Computer Science & Engineering', 1, NOW()),
(3, 'Jane Smith', 'jane.smith@campus.edu', '$2a$10$7ZkQ6zNq2z0VvB1zJc0yRe0wKq.LgZ0cE2Kq7K4B5eT6N8Q1yO0qS', 'STUDENT', 'STU-2024-002', '+91 9876543212', 'Information Technology', 1, NOW()),
(4, 'Raj Patel', 'raj.patel@campus.edu', '$2a$10$7ZkQ6zNq2z0VvB1zJc0yRe0wKq.LgZ0cE2Kq7K4B5eT6N8Q1yO0qS', 'STUDENT', 'STU-2024-003', '+91 9876543213', 'Electronics & Communication', 1, NOW())
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- Diverse Campus Events
INSERT INTO events (id, title, description, category, venue, start_datetime, end_datetime, registration_deadline, capacity, registered_count, status, organizer_id, created_at)
VALUES
(1, 'National Tech Symposium: InnovateX 2026', 
 'InnovateX 2026 brings together industry pioneers, researchers, and aspiring student engineers for an immersive full-day symposium covering cutting-edge artificial intelligence, distributed cloud systems, and quantum computing. Features keynote addresses, project exhibitions, and technical paper presentations.', 
 'TECHNICAL', 'Dr. A.P.J. Abdul Kalam Auditorium', 
 DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 343 HOUR), 
 DATE_ADD(NOW(), INTERVAL 10 DAY), 120, 2, 'OPEN', 1, NOW()),

(2, 'Hands-on Generative AI & LLM Engineering Workshop', 
 'Intensive practical masterclass on developing real-world enterprise applications with Large Language Models, Retrieval-Augmented Generation (RAG), and vector embeddings. Participants will build and deploy intelligent agents using modern Java frameworks.', 
 'WORKSHOP', 'Advanced Computing Lab 3', 
 DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 486 HOUR), 
 DATE_ADD(NOW(), INTERVAL 18 DAY), 45, 1, 'OPEN', 1, NOW()),

(3, 'Campus Hackathon: CodeSprint 36-Hours', 
 'An exhilarating 36-hour non-stop hackathon challenging multidisciplinary student teams to prototype innovative software and IoT solutions for Smart Campus, Healthcare, and Sustainable Green Technologies. Exciting prizes and mentorship from tech leaders.', 
 'HACKATHON', 'Innovation & Incubation Hub', 
 DATE_ADD(NOW(), INTERVAL 25 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY), 
 DATE_ADD(NOW(), INTERVAL 22 DAY), 60, 1, 'OPEN', 1, NOW()),

(4, 'Annual Cultural Extravaganza: Tarang 2026', 
 'The premier cultural event of the year featuring music concerts, classical and contemporary dance competitions, theatrical drama performances, and fine arts exhibitions. Celebrate student talent and artistic expression across campuses.', 
 'CULTURAL', 'Open Air Theatre (OAT)', 
 DATE_ADD(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 728 HOUR), 
 DATE_ADD(NOW(), INTERVAL 28 DAY), 300, 0, 'OPEN', 1, NOW()),

(5, 'Campus Placement Aptitude & Coding Masterclass', 
 'Comprehensive training bootcamp designed to equip pre-final and final year students with data structures, algorithmic problem solving, mock technical interviews, and HR communication strategies for upcoming campus placement drives.', 
 'PLACEMENT', 'Seminar Hall B', 
 DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 172 HOUR), 
 DATE_ADD(NOW(), INTERVAL 5 DAY), 80, 0, 'OPEN', 1, NOW()),

(6, 'Inter-Department Football Championship', 
 'Annual intra-college knockout football tournament showcasing athleticism and sportsmanship. Teams representing all academic departments compete for the prestigious Chancellor Shield.', 
 'SPORTS', 'Main College Sports Complex Ground', 
 DATE_ADD(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 293 HOUR), 
 DATE_ADD(NOW(), INTERVAL 9 DAY), 50, 0, 'OPEN', 1, NOW()),

(7, 'Emerging Trends in Cybersecurity & Zero Trust Architecture', 
 'Distinguished guest lecture by cybersecurity professionals examining zero trust principles, identity management, penetration testing fundamentals, and modern defensive security tactics.', 
 'SEMINAR', 'Conference Hall 1', 
 DATE_SUB(NOW(), INTERVAL 120 HOUR), DATE_SUB(NOW(), INTERVAL 117 HOUR), 
 DATE_SUB(NOW(), INTERVAL 192 HOUR), 60, 2, 'COMPLETED', 1, NOW())
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- Sample Registrations
INSERT INTO registrations (id, registration_reference, student_id, event_id, registration_status, qr_code_token, registered_at, created_at)
VALUES
(1, 'REG-20261029-71A89B', 2, 1, 'CONFIRMED', 'TKN-INNOVATEX-JOHN-DOE-001', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(2, 'REG-20261029-82C45D', 3, 1, 'CONFIRMED', 'TKN-INNOVATEX-JANE-SMITH-002', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(3, 'REG-20261029-93E12F', 2, 2, 'CONFIRMED', 'TKN-GENAI-JOHN-DOE-003', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(4, 'REG-20261029-14A56E', 4, 3, 'CONFIRMED', 'TKN-CODESPRINT-RAJ-004', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(5, 'REG-20261020-55B78A', 2, 7, 'CONFIRMED', 'TKN-CYBER-JOHN-DOE-005', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(6, 'REG-20261020-66C89B', 3, 7, 'CONFIRMED', 'TKN-CYBER-JANE-SMITH-006', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW())
ON DUPLICATE KEY UPDATE registration_reference=VALUES(registration_reference);

-- Sample Attendance
INSERT INTO attendance (id, registration_id, attendance_status, checked_in_at, checked_in_by, notes, created_at)
VALUES
(1, 1, 'ABSENT', NULL, NULL, 'Event upcoming', NOW()),
(2, 2, 'ABSENT', NULL, NULL, 'Event upcoming', NOW()),
(3, 3, 'ABSENT', NULL, NULL, 'Event upcoming', NOW()),
(4, 4, 'ABSENT', NULL, NULL, 'Event upcoming', NOW()),
(5, 5, 'PRESENT', DATE_SUB(NOW(), INTERVAL 7230 MINUTE), 1, 'Checked in at registration counter', NOW()),
(6, 6, 'PRESENT', DATE_SUB(NOW(), INTERVAL 7245 MINUTE), 1, 'Checked in at registration counter', NOW())
ON DUPLICATE KEY UPDATE attendance_status=VALUES(attendance_status);

-- Sample Audit Logs
INSERT INTO audit_logs (id, actor_user_id, action, entity_type, entity_id, timestamp, details)
VALUES
(1, 1, 'EVENT_CREATED', 'CampusEvent', 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 'Created event InnovateX 2026'),
(2, 1, 'EVENT_PUBLISHED', 'CampusEvent', 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 'Status changed to OPEN'),
(3, 1, 'ATTENDANCE_MARKED', 'Attendance', 5, DATE_SUB(NOW(), INTERVAL 5 DAY), 'Marked PRESENT for registration REG-20261020-55B78A'),
(4, 1, 'ATTENDANCE_MARKED', 'Attendance', 6, DATE_SUB(NOW(), INTERVAL 5 DAY), 'Marked PRESENT for registration REG-20261020-66C89B')
ON DUPLICATE KEY UPDATE action=VALUES(action);

-- Sample Notifications
INSERT INTO notifications (id, user_id, notification_type, subject, message, read_status, created_at)
VALUES
(1, 2, 'REGISTRATION_CONFIRMED', 'Registration Confirmed: InnovateX 2026', 'You have successfully registered for National Tech Symposium: InnovateX 2026. Your Registration Reference is REG-20261029-71A89B.', 0, NOW()),
(2, 3, 'REGISTRATION_CONFIRMED', 'Registration Confirmed: InnovateX 2026', 'You have successfully registered for National Tech Symposium: InnovateX 2026. Your Registration Reference is REG-20261029-82C45D.', 0, NOW()),
(3, 2, 'ATTENDANCE_RECORDED', 'Attendance Marked: Cybersecurity Seminar', 'Your attendance was marked as PRESENT for Emerging Trends in Cybersecurity.', 1, DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE subject=VALUES(subject);
