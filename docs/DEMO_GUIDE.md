# College Demonstration & Evaluator Walkthrough Guide

## 1. Demonstration Credentials Matrix

| Role | Email Address | Password | Default Portal / Entry Point | Access Scope |
|---|---|---|---|---|
| **Administrator** | `admin@campus.edu` | `Admin@123` | `http://localhost:8080/admin/dashboard` | Full system control, analytics, waitlist, audits, bulk certificate issuance |
| **Event Organizer** | `organizer@campus.edu` | `Organizer@123` | `http://localhost:8080/organizer/dashboard` | Assigned event roster, QR attendance desk, feedback review |
| **Student 1** | `john.doe@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Registrations, waitlist status, certificates, feedback, tickets |
| **Student 2** | `jane.smith@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Registrations, waitlist status, certificates, tickets |
| **Student 3** | `raj.patel@campus.edu` | `Student@123` | `http://localhost:8080/student/dashboard` | Registrations, tickets |
| **Public / Any** | *Anonymous* | *N/A* | `http://localhost:8080/verify-certificate` | Public credential verification, event discovery |

---

## 2. Enterprise Feature Walkthrough Script

### Step 1: Public Event Discovery & Conflict Detection (1.5 minutes)
1. Open browser to `http://localhost:8080/`.
2. Browse upcoming events and search by title, category, and venue.
3. Log in as `john.doe@campus.edu`. Open an event happening at the same time as an event John is already attending.
4. **Demonstrate Conflict Detection**: An explicit amber warning banner appears: *"Schedule Conflict Warning: You already have an active registration for another event during this time window."*

### Step 2: Smart Event Waitlist Lifecycle (2 minutes)
1. Find an event that has reached full capacity.
2. Note that the standard registration button is replaced by **"Join Event Waitlist"**.
3. Click "Join Event Waitlist". The student is immediately placed in queue with a unique position (e.g. `#1`).
4. On the student dashboard, inspect the dedicated **"My Waitlist Positions & Seat Offers"** card.
5. Demonstrate Cancellation & Auto-Promotion: When an existing attendee cancels, the seat is atomically held for the next waiting student with a 24-hour expiration window.
6. The waitlist card updates to **"SEAT OFFERED! (Expires: ...)"** with one-click **"Claim Seat Now"** and **"Decline"** buttons.
7. Upon clicking "Claim Seat Now", the seat is confirmed and converted into an official registration ticket.

### Step 3: Venue Check-in Desk & Multi-Status Validation (2 minutes)
1. Log in as Administrator (`admin@campus.edu`) or Organizer (`organizer@campus.edu`).
2. Navigate to **Manage Events** &rarr; click **"Roster"** or **"Check-in Desk"**.
3. **Valid Check-in**: Input an attendee's Registration Reference ID or scan the QR token.
   - Result: High-contrast green alert: *"Check-in successful! Welcome, [Student Name]"*, with timestamp and staff badge.
4. **Duplicate Check-in Prevention**: Re-scan the same reference ID.
   - Result: High-contrast amber alert: *"Warning: [Student] was ALREADY checked in at [Time] by [Staff]"*.
5. **Wrong Event Protection**: Attempt to scan a ticket registered for Event B at Event A's desk.
   - Result: Red alert: *"Wrong event! Ticket is registered for Event B, but this desk is for Event A"*.
6. **Cancelled Ticket Rejection**: Attempt to scan a cancelled registration ticket.
   - Result: Red alert: *"Check-in rejected: Registration was CANCELLED"*.

### Step 4: Downloadable OpenPDF Participation Certificates (1.5 minutes)
1. In the attendance roster, verify that attendees marked **PRESENT** are eligible for certificates.
2. Click **"Issue Certificates (Present)"** to bulk-generate certificates for all verified attendees.
3. Log in as the student &rarr; click **"Certificates"** in the navigation bar.
4. Review the issued certificate record with its verifiable code (format: `CERT-YYYYMMDD-XXXX`).
5. Click **"Download PDF"**: A beautifully styled landscape A4 certificate opens with official college header, recipient name, event details, seal, and coordinator signatures.

### Step 5: Public Credential Verification Portal (1 minute)
1. Open an Incognito/Anonymous browser tab to `http://localhost:8080/verify-certificate`.
2. Enter the certificate code (e.g. `CERT-20261029-D281`).
3. Click **"Verify Code"**.
4. The system validates directly against MySQL and renders the official green **"Official Verified Credential"** card with student identity and verified attendance status.
5. Enter a random invalid code to verify that tampering or invalid codes are safely rejected.

### Step 6: Post-Event Feedback & 1-5 Star Ratings (1.5 minutes)
1. In the student dashboard under **"Event Participation History"**, locate an event marked **PRESENT**.
2. Click **"★ Feedback"**.
3. Select a 1–5 star rating and enter review comments.
4. Submit the review: The system prevents duplicates (1 review per student per event).
5. Open the event page or admin feedback view (`/admin/feedback/{id}`) to observe average rating scores and review listings.

### Step 7: Advanced Analytics Dashboard & Reports (2 minutes)
1. Log in as Administrator &rarr; click **"Analytics"** in the navigation bar (`/admin/analytics`).
2. Review the live database-driven KPI cards:
   - Total Registrations, Confirmed, Cancelled
   - Verified Attendance Turnout Rate (%)
   - Capacity Utilization (%)
   - Cancellation Rate (%) & No-Show Rate (%)
3. Review pure CSS visual distribution bars for **Category-Wise** and **Department-Wise** participation.
4. Review the **Event Performance & Attendance Turnout Comparison** table.
5. Apply filters (Category dropdown, Date Range from/to) &rarr; click **"Apply Filters"**.
6. Click **"Export CSV"** and **"Export Excel (.xlsx)"**: The generated files match the active filters with formulas and multi-sheet layouts.

### Step 8: Organizer Role Delegation & Security Isolation (1.5 minutes)
1. Log in as `organizer@campus.edu` / `Organizer@123`.
2. Notice the organizer dashboard only shows events assigned to this faculty member.
3. Access the organizer check-in desk for their assigned event (`/organizer/events/2/attendance`).
4. Attempt to access `/admin/dashboard`: The system immediately blocks access with **403 Forbidden**.
5. Attempt to access an event assigned to someone else: Access is strictly denied.
