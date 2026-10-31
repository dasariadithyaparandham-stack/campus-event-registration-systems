# System Architecture & Technical Design

## 1. High-Level Architectural Overview

The **Campus Event Registration System** is engineered as a robust, layered, server-side rendered enterprise web application built on **Java 17**, **Spring Boot 3.2.5**, **Spring MVC**, **Spring Data JPA / Hibernate 6**, **Spring Security 6**, and **MySQL 8.0**.

The application adheres to clean architecture principles with clear separation of concerns across presentation, security, application business logic, persistence, and external reporting utilities.

```mermaid
graph TD
    subgraph ClientLayer ["Client Presentation Layer (Browser)"]
        UI_Desktop["Desktop Web Browser (HTML5 / CSS3 / Responsive)"]
        UI_Mobile["Mobile / Tablet Browser (Responsive Cards & Navigation)"]
        Print_Pass["Physical / Digital Print Pass (@media print)"]
    end

    subgraph SecurityLayer ["Security & Gateway Layer"]
        SecFilter["Spring Security Filter Chain"]
        CSRF["CSRF Protection Token Validator"]
        AuthProv["DaoAuthenticationProvider (BCrypt 10 rounds)"]
        RoleAuth["Role-Based Access Control (ROLE_STUDENT, ROLE_ADMIN)"]
    end

    subgraph PresentationLayer ["Spring MVC Controller Layer"]
        HomeController["HomeController (/, /events, /events/{id})"]
        AuthController["AuthController (/login, /register, /access-denied)"]
        StudentController["StudentController (/student/**)"]
        AdminController["AdminController (/admin/**)"]
    end

    subgraph ServiceLayer ["Business Logic Service Layer (@Transactional)"]
        EventService["EventService (CRUD, Status Transitions, Capacity Rules)"]
        RegService["RegistrationService (Pessimistic Lock, Atomic Reservation, Cancellation)"]
        AttService["AttendanceService (QR Validation, Rapid Check-in, Correction)"]
        RepService["ReportService (CSV & Excel .xlsx Generation, Utilization Stats)"]
        NotifService["NotificationService (In-app Alerts, User Messages)"]
        AuditService["AuditService (Immutable Security Audit Logging)"]
        UserService["UserService (BCrypt Student Registration, Profile Mgmt)"]
    end

    subgraph UtilityLayer ["External Processing & Utilities"]
        ZXing["ZXing Library (QR Code Matrix Generation)"]
        POI["Apache POI (Excel .xlsx Workbook Generator)"]
        CSVUtil["CsvReportGenerator (CSV Injection Sanitizer)"]
    end

    subgraph PersistenceLayer ["Spring Data JPA & Hibernate 6"]
        UserRepo["UserRepository"]
        EventRepo["EventRepository (Pessimistic Lock findByIdForUpdate)"]
        RegRepo["RegistrationRepository (Search, Status Filtering)"]
        AttRepo["AttendanceRepository (Turnout Aggregation)"]
        AuditRepo["AuditLogRepository (Audit Log Stream)"]
        NotifRepo["NotificationRepository"]
    end

    subgraph DatabaseLayer ["Relational Storage (MySQL 8)"]
        DB[(MySQL 8 Database: campus_events)]
        Flyway["Flyway Migration Engine (V1 Initial Schema, V2 Seed Data)"]
    end

    UI_Desktop --> SecFilter
    UI_Mobile --> SecFilter
    SecFilter --> CSRF --> RoleAuth --> AuthProv
    RoleAuth --> HomeController & AuthController & StudentController & AdminController

    StudentController --> RegService & UserService & NotifService
    AdminController --> EventService & AttService & RepService & AuditService
    HomeController --> EventService & RegService

    RegService --> ZXing
    RepService --> POI & CSVUtil

    EventService --> EventRepo
    RegService --> EventRepo & RegRepo & AttRepo
    AttService --> AttRepo & RegRepo
    UserService --> UserRepo
    AuditService --> AuditRepo
    NotifService --> NotifRepo

    EventRepo & RegRepo & AttRepo & UserRepo & AuditRepo & NotifRepo --> DB
    Flyway -.->|Controls Migrations| DB
```

---

## 2. Concurrency Model & Pessimistic Locking

To strictly eliminate race conditions, duplicate bookings, and overbooking when multiple students attempt to register simultaneously for the final seat in a high-demand event, the application utilizes database-level **Pessimistic Locking**:

```mermaid
sequenceDiagram
    autonumber
    actor S1 as Student 1 (Concurrent)
    actor S2 as Student 2 (Concurrent)
    participant Ctrl as RegistrationController
    participant Svc as RegistrationService (@Transactional)
    participant Repo as EventRepository
    participant DB as MySQL Database (events table)

    S1->>Ctrl: POST /student/register/1
    S2->>Ctrl: POST /student/register/1
    
    Ctrl->>Svc: registerStudentForEvent(Student1, Event1)
    Ctrl->>Svc: registerStudentForEvent(Student2, Event1)
    
    Note over Svc,DB: S1 initiates transaction & requests Lock
    Svc->>Repo: findByIdForUpdate(1L)
    Repo->>DB: SELECT * FROM events WHERE id=1 FOR UPDATE
    DB-->>Svc: Event acquired (Lock granted to S1)

    Note over Svc,DB: S2 arrives & requests Lock (BLOCKED by MySQL InnoDB)
    Svc->>Repo: findByIdForUpdate(1L)
    Repo->>DB: SELECT * FROM events WHERE id=1 FOR UPDATE (Waiting...)

    Svc->>Svc: Check capacity (e.g., 49 / 50) -> OK
    Svc->>Svc: Generate REG-REF & QR Token
    Svc->>DB: INSERT into registrations & UPDATE registered_count = 50
    DB-->>Svc: Success
    Note over Svc,DB: S1 commits transaction -> Lock Released

    DB-->>Svc: Lock now granted to S2
    Svc->>Svc: Check capacity (50 / 50) -> FULL!
    Svc-->>Ctrl: Throw RegistrationException("Event reached maximum capacity")
    Ctrl-->>S2: HTTP Redirect with flash error: Event Full
```

---

## 3. Security Architecture & Role Isolation

```mermaid
graph LR
    subgraph PublicEndpoints ["Public / Anonymous (/ /events /login /register)"]
        P1["Event Catalog"]
        P2["Event Details"]
        P3["Account Registration"]
        P4["Session Login"]
    end

    subgraph StudentEndpoints ["Student Scope (/student/**) - ROLE_STUDENT"]
        S1["My Dashboard"]
        S2["Active Registrations"]
        S3["Printable QR Ticket"]
        S4["Cancel Registration"]
        S5["Profile Management"]
    end

    subgraph AdminEndpoints ["Admin Scope (/admin/**) - ROLE_ADMIN"]
        A1["Admin Console & Metrics"]
        A2["Event CRUD & Lifecycle"]
        A3["Participant Rosters"]
        A4["QR Check-in Console"]
        A5["CSV / Excel Export Center"]
        A6["Audit Activity Log"]
    end
```

- **Role Protection**: Server-side enforcement with `hasRole('ADMIN')` and `hasRole('STUDENT')`.
- **Ownership Verification**: Before displaying or cancelling a ticket, `registration.getStudent().getId().equals(currentUser.getId())` is verified on the backend.
- **CSRF Tokens**: All state-modifying requests (`POST`) require a cryptographically valid CSRF token.
- **Password Hashing**: BCrypt with strength factor 10.

