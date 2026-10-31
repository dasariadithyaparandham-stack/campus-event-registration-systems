# Database Entity-Relationship (ER) Design

## 1. Relational Entity Diagram

```mermaid
erDiagram
    USERS ||--o{ EVENTS : organizes
    USERS ||--o{ REGISTRATIONS : registers
    USERS ||--o{ ATTENDANCE : checks_in
    USERS ||--o{ AUDIT_LOGS : performs
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ WAITLIST : waits_in
    USERS ||--o{ CERTIFICATES : receives
    USERS ||--o{ EVENT_FEEDBACK : provides

    EVENTS ||--o{ REGISTRATIONS : contains
    EVENTS ||--o{ WAITLIST : queues
    EVENTS ||--o{ CERTIFICATES : generates
    EVENTS ||--o{ EVENT_FEEDBACK : reviews

    REGISTRATIONS ||--|| ATTENDANCE : tracks
    REGISTRATIONS ||--o| CERTIFICATES : earns
    REGISTRATIONS ||--o| EVENT_FEEDBACK : evaluates

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        varchar role "STUDENT | ADMIN | ORGANIZER"
        varchar student_id_number
        varchar phone
        varchar department
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    EVENTS {
        bigint id PK
        varchar title
        text description
        varchar category
        varchar venue
        datetime start_datetime
        datetime end_datetime
        datetime registration_deadline
        int capacity
        int registered_count
        varchar status "DRAFT | OPEN | CLOSED | CANCELLED | COMPLETED"
        bigint organizer_id FK
        timestamp created_at
        timestamp updated_at
    }

    REGISTRATIONS {
        bigint id PK
        varchar registration_reference UK
        bigint student_id FK
        bigint event_id FK
        varchar registration_status "CONFIRMED | CANCELLED"
        varchar qr_code_token UK
        varchar active_key UK "Generated: student_id - event_id if CONFIRMED"
        datetime registered_at
        datetime cancelled_at
        timestamp created_at
        timestamp updated_at
    }

    ATTENDANCE {
        bigint id PK
        bigint registration_id FK,UK
        varchar attendance_status "PRESENT | ABSENT"
        datetime checked_in_at
        bigint checked_in_by FK
        varchar notes
        timestamp created_at
        timestamp updated_at
    }

    WAITLIST {
        bigint id PK
        bigint event_id FK
        bigint student_id FK
        int queue_position
        varchar status "WAITING | OFFERED | ACCEPTED | EXPIRED | DECLINED | CANCELLED"
        datetime joined_at
        datetime offered_at
        datetime offer_expires_at
        datetime responded_at
        varchar active_waitlist_key UK "Generated: student_id - event_id if WAITING or OFFERED"
        timestamp created_at
        timestamp updated_at
    }

    CERTIFICATES {
        bigint id PK
        varchar certificate_code UK "Format: CERT-YYYYMMDD-XXXX"
        bigint registration_id FK,UK
        bigint student_id FK
        bigint event_id FK
        varchar student_name
        varchar event_title
        datetime event_date
        datetime issued_at
        varchar status "ACTIVE | REVOKED"
        timestamp created_at
    }

    EVENT_FEEDBACK {
        bigint id PK
        bigint event_id FK
        bigint student_id FK
        bigint registration_id FK,UK
        int rating "1 to 5 stars"
        text comments
        timestamp created_at
    }

    AUDIT_LOGS {
        bigint id PK
        bigint actor_user_id FK
        varchar action
        varchar entity_type
        bigint entity_id
        datetime timestamp
        text details
    }

    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar notification_type
        varchar subject
        text message
        boolean read_status
        timestamp created_at
    }
```

---

## 2. Table Specifications & Integrity Rules

### `users` Table
Stores authentication credentials, contact information, and role authorizations.
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraints**: `email` (Unique college email address)
- **Indexes**: `idx_users_email`, `idx_users_role`
- **Security Rule**: The `role` column accepts `STUDENT`, `ADMIN`, or `ORGANIZER`. Public signups can only ever populate `STUDENT`.

### `events` Table
Stores campus event listings, scheduling constraints, capacity quotas, and lifecycle states.
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Foreign Key**: `organizer_id` -> `users(id)`
- **Indexes**: `idx_events_status`, `idx_events_category`, `idx_events_start_datetime`, `idx_events_deadline`
- **Status Lifecycle**: `DRAFT` &rarr; `OPEN` &rarr; `CLOSED` / `CANCELLED` &rarr; `COMPLETED`.

### `registrations` Table
Tracks ticket issuances, student reservations, and status (`CONFIRMED` or `CANCELLED`).
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraints**:
  - `registration_reference`: Human-readable identifier (e.g., `REG-20261029-71A89B`)
  - `qr_code_token`: Opaque UUID string encoded into ticket QR codes
  - `active_key`: Generated STORED column:
    ```sql
    CASE WHEN registration_status = 'CONFIRMED' THEN CONCAT(student_id, '-', event_id) ELSE NULL END
    ```
- **Foreign Keys**:
  - `student_id` -> `users(id)`
  - `event_id` -> `events(id)`

### `attendance` Table
Records participant verification at the venue registration desk.
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraint**: `registration_id` (1:1 with registration)
- **Foreign Keys**:
  - `registration_id` -> `registrations(id)`
  - `checked_in_by` -> `users(id)` (Staff member performing check-in)
- **Status**: `PRESENT` or `ABSENT`.

### `waitlist` Table (Enhancement A)
Maintains FIFO waitlist positions for events at maximum capacity.
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Generated Key Constraint**: `active_waitlist_key`:
  ```sql
  CASE WHEN status IN ('WAITING', 'OFFERED') THEN CONCAT(student_id, '-', event_id) ELSE NULL END
  ```
  Guarantees zero duplicate active waitlist entries per student per event.
- **Indexes**: `(event_id, status)`, `(event_id, queue_position)`, `(student_id)`.

### `certificates` Table (Enhancement E)
Stores participation credentials issued strictly to verified attendees (`PRESENT`).
- **Primary Key**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraints**:
  - `certificate_code`: Verifiable public code (`CERT-YYYYMMDD-XXXX`)
  - `registration_id`: 1 certificate per registration
- **Indexes**: `idx_cert_code`, `idx_cert_student`, `idx_cert_event`.

### `event_feedback` Table (Enhancement H)
Stores verified participant ratings (1–5 stars) and critiques.
- **Unique Constraints**:
  - `registration_id` (1 review per registration)
  - `uq_student_event_feedback`: `(student_id, event_id)` (1 review per student per event).
- **Indexes**: `idx_feedback_event`, `idx_feedback_rating`.
