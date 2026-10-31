# Software Requirements Specification (SRS)

## 1. Introduction

### 1.1 Purpose
This document provides a comprehensive specification of requirements for the **Campus Event Registration System**, a web-based enterprise application built for collegiate institutions to manage the complete lifecycle of campus events, registrations, attendance, and reporting.

### 1.2 Scope
The system automates the following key operational areas:
- Discovery, categorization, and full-text searching of technical, academic, and cultural events.
- Concurrency-safe seat reservation with atomic capacity enforcement.
- Cryptographically sound user authentication and role-based access control.
- Digital entry pass generation with embedded QR codes.
- Fast venue check-in verification and attendance tracking.
- Multi-format data exports (CSV and Microsoft Excel `.xlsx`) for institutional compliance and reporting.
- Tamper-evident activity audit logging.

---

## 2. Overall Description

### 2.1 Product Perspective
The system operates as an autonomous, self-contained Spring Boot 3 web application backed by a MySQL 8 relational database. It is accessible across standard web browsers without requiring browser extensions or client-side JavaScript frameworks.

### 2.2 User Classes & Characteristics
- **Anonymous Visitors**: Prospective students or unauthenticated users browsing upcoming college events.
- **Students**: Enrolled students authorized to register for events, download passes, and view attendance records.
- **Administrators**: Department organizers and college administrative staff authorized to create events, oversee participants, conduct check-ins, and export institutional reports.

---

## 3. Specific Functional Requirements

| ID | Module | Description |
|---|---|---|
| **FR-01** | **Authentication** | The system shall provide secure sign-up for students with BCrypt password hashing (10 rounds). Public registration shall never grant administrative privileges. |
| **FR-02** | **Authorization** | The system shall enforce role-based access control (RBAC) on the server side: `/admin/**` restricted to `ROLE_ADMIN`, `/student/**` restricted to `ROLE_STUDENT`. |
| **FR-03** | **Event Management** | Administrators shall be able to create, edit, publish (`OPEN`), close (`CLOSED`), cancel (`CANCELLED`), or complete (`COMPLETED`) events. |
| **FR-04** | **Capacity & Deadlines** | The system shall strictly prevent registrations when: (a) capacity is reached, (b) deadline has passed, (c) event has already started, or (d) event status is not `OPEN`. |
| **FR-05** | **Concurrency Protection** | The system shall use database-level pessimistic locking (`PESSIMISTIC_WRITE`) during registration to prevent race conditions and overbooking. |
| **FR-06** | **Duplicate Prevention** | The system shall prevent duplicate active registrations for the same student-event pair using both service validation and a unique database constraint. |
| **FR-07** | **Unique Identifiers** | Each confirmed registration shall receive a unique, non-guessable reference code (`REG-YYYYMMDD-XXXXXX`) and an opaque QR token. |
| **FR-08** | **Digital QR Tickets** | The system shall generate a high-contrast QR code image for each ticket and provide a printable boarding-pass layout with `@media print` styling. |
| **FR-09** | **Ticket Ownership** | The system shall verify ownership on the server to prevent students from viewing or downloading another student's ticket. |
| **FR-10** | **Desk Check-in** | Administrators shall be able to mark participants present by scanning the QR code token or typing the registration reference. |
| **FR-11** | **Attendance Reversal** | Administrators shall be permitted to toggle attendance status to correct accidental check-in errors. |
| **FR-12** | **Reporting & Exports** | Administrators shall be able to export event rosters and campus event summaries in CSV and Microsoft Excel (`.xlsx`) formats with formula injection sanitization. |

---

## 4. Non-Functional Requirements

### 4.1 Security Requirements
- **Password Protection**: Passwords must never be stored in plaintext. Passwords must be hashed using BCrypt.
- **CSRF Defense**: All state-modifying requests (`POST`, `PUT`, `DELETE`) must validate a cryptographically secure CSRF token.
- **Injection Defense**:
  - SQL Injection: Mitigated through parameterized Spring Data JPA queries.
  - CSV Injection: Mitigated by prepending single quotes to formulas (`=`, `+`, `-`, `@`).
  - XSS: Mitigated through Thymeleaf's automatic HTML escaping.
- **Least Privilege**: Sensitive user fields cannot be mass-assigned from submitted web forms.

### 4.2 Performance & Scalability
- **Page Response Time**: All primary views render in under 200 ms under standard database loads.
- **Connection Pooling**: HikariCP connection pool with optimized maximum pool size (10 connections) and 20-second connection timeout.
- **Database Indexing**: Essential lookup columns (`email`, `status`, `category`, `start_datetime`, `registration_deadline`, `student_id`, `event_id`) are indexed in MySQL.

### 4.3 Reliability & Database Integrity
- **Transactional Consistency**: All booking, cancellation, and check-in routines execute inside `@Transactional` boundaries.
- **Zero Data Loss**: Cancellation marks records as `CANCELLED` rather than physically deleting rows, preserving institutional reporting integrity.

