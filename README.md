# Campus Event Registration System

> **A Production-Grade Java-Based Web Application for College Event Management**  
> *Built with Java 17, Spring Boot 3.2.5, Spring MVC, Thymeleaf, Spring Security 6, Spring Data JPA, Hibernate, and MySQL 8.*

---

## 1. Project Overview

The **Campus Event Registration System** is an end-to-end, enterprise-ready, database-driven web application designed to digitize the complete campus event lifecycle across colleges and universities.

It eliminates manual paper registrations, duplicate bookings, overbooking, and inaccurate attendance records by offering:
- **Student Portal**: Discover campus events, filter by category/venue/date, register with instant seat allocation, view/print digital QR-code entry passes, manage reservations, join smart FIFO waitlists, track participation history, download PDF participation certificates, and submit 1–5 star reviews.
- **Organizer Portal**: Faculty coordinators manage assigned events, access real-time check-in consoles, monitor attendee rosters, and inspect participant feedback.
- **Administrator Portal**: Real-time KPI dashboards, event lifecycle management (Draft, Open, Closed, Cancelled, Completed), venue check-in via QR-code scanners, advanced database-driven analytics, waitlist queue management, bulk certificate issuance, CSV & Excel (`.xlsx`) report exports, and security audit logging.
- **Enterprise Architecture**: Database-level **Pessimistic Locking** (`SELECT ... FOR UPDATE`) prevents race conditions and overbooking. Partial unique constraints in MySQL eliminate duplicate active student-event bookings and duplicate waitlist entries.

---

## 2. Advanced Upgrades Implemented

- **Enhancement A: Smart Event Waitlist**: Database-backed FIFO waitlist with unique queue positions, atomic seat reservation, 24-hour offer expiration windows, student claim/decline workflows, and automated next-in-line promotion upon cancellations.
- **Enhancement B: Secure QR-Code Check-In**: Opaque UUID tokens stored in MySQL, distinct multi-status evaluation (`VALID`, `ALREADY_CHECKED_IN`, `CANCELLED`, `WRONG_EVENT`, `INVALID_TOKEN`), visual desk feedback, timestamp and staff member tracking.
- **Enhancement C: Advanced Event Analytics**: Database aggregated metrics (Turnout %, Capacity Utilization %, Cancellation %, No-show %), pure CSS visual progress meters, category/department distributions, date-range & category filters, and CSV & Excel (`.xlsx`) exports matching active filters.
- **Enhancement D: Security & Concurrency Audit**: Brute-force login attempt lockout (account blocked after 5 failed attempts for 15 minutes), session fixation migration (`sessionFixation().migrateSession()`), strict role separation, BCrypt password hashing, and CSRF protection.
- **Enhancement E: Downloadable Participation Certificates**: OpenPDF landscape A4 vector certificate generator, strictly restricted to attendees marked `PRESENT`, verifiable codes (`CERT-YYYYMMDD-XXXX`), and a public `/verify-certificate?code=...` verification portal.
- **Enhancement F: Notification Center**: In-app notification hub with unread badge counters, mark-as-read / read-all workflows, and structured email delivery abstraction with graceful logging fallback.
- **Enhancement G: Event Conflict Detection**: Automatic overlapping schedule analysis for active registrations, 2-hour default duration fallback for untimed events, and proactive amber warnings on event details.
- **Enhancement H: Post-Event Feedback & Ratings**: 1–5 star ratings and reviews restricted to verified attendees (`PRESENT`), duplicate prevention (1 review per student per event), and organizer summary dashboards.
- **Enhancement I: Organizer Role (`ROLE_ORGANIZER`)**: Strict role-based isolation allowing faculty organizers to oversee only their assigned events without institutional admin access.
- **Enhancement J: Database & Performance Optimization**: Flyway migration `V3__advanced_features.sql`, composite indexing on active waitlist and certificates, and zero in-memory full-table scans.

---

## 3. Technology Stack

- **Language**: Java 17 (OpenJDK / Eclipse Temurin)
- **Backend Framework**: Spring Boot 3.2.5
- **Web & Template Engine**: Spring MVC + Thymeleaf (Server-rendered HTML5 & CSS3)
- **Security & Cryptography**: Spring Security 6 with BCrypt password hashing, session fixation defense, and CSRF protection
- **Database & Persistence**: MySQL 8.0, Spring Data JPA, Hibernate 6, HikariCP
- **Database Migration**: Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`, `V3__advanced_features.sql`)
- **Validation**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Min`, `@Max`)
- **QR Code Generation**: ZXing (`com.google.zxing:core`, `com.google.zxing:javase`)
- **PDF Certificate Generation**: OpenPDF 1.3.39 (`com.github.librepdf:openpdf`)
- **Excel & CSV Reporting**: Apache POI 5.2.5 (`org.apache.poi:poi-ooxml`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test, H2 (48 Automated Tests - 100% Pass)
- **Build Tool**: Apache Maven 3.9+

---

## 4. Demonstration Accounts Matrix

| Role | Email Address | Password | Default Portal | Capabilities |
|---|---|---|---|---|
| **Administrator** | `admin@campus.edu` | `Admin@123` | `http://localhost:8080/admin/dashboard` | Full control, analytics, bulk certificate issuance, audits |
| **Event Organizer** | `organizer@campus.edu` | `Organizer@123` | `http://localhost:8080/organizer/dashboard` | Check-in desk & roster for assigned events |
| **Student 1** | `john.doe@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Bookings, waitlist, ticket download, feedback |
| **Student 2** | `jane.smith@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Bookings, waitlist, certificates |
| **Student 3** | `raj.patel@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Bookings, tickets |
| **Public User** | *Anonymous* | *N/A* | `http://localhost:8080/verify-certificate` | Public credential verification, event catalog |

---

## 5. Quick Start Instructions (Windows)

### Step 1: Ensure Prerequisites
Ensure **Java 17**, **Maven**, and **MySQL 8** are available in your PATH:
```powershell
java -version
mvn -version
mysql --version
```

### Step 2: Configure Database
Log in to MySQL and ensure the database exists:
```sql
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update `src/main/resources/application.properties` if your MySQL password differs from `Adithya@123`:
```properties
spring.datasource.username=root
spring.datasource.password=YourPassword
```

### Step 3: Run the Automated Test Suite
```powershell
mvn clean test
```
All **48 tests** will run against an isolated in-memory test database, validating architecture, security rules, and business logic.

### Step 4: Start the Application
```powershell
mvn spring-boot:run
```
Flyway will automatically execute migrations `V1`, `V2`, and `V3` to configure all tables, constraints, indexes, and seed records.

Access the application in your browser:
**`http://localhost:8080/`**

---

## 6. Project Documentation Links

- [System Architecture & Class Hierarchy](docs/ARCHITECTURE.md)
- [Database Schema & Entity-Relationship Diagram](docs/DATABASE_ERD.md)
- [Demonstration & Evaluator Walkthrough Guide](docs/DEMO_GUIDE.md)
- [Automated Test Suite & Quality Report](docs/TESTING.md)
- [Software Requirements Specification (SRS)](docs/SRS.md)
- [Functional Use Cases](docs/USE_CASES.md)
