package com.campus.events.util;

import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventRegistration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CsvReportGenerator() {
    }

    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        // Prevent CSV Formula Injection (=, +, -, @, \t, \r)
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@") || clean.startsWith("\t")) {
            clean = "'" + clean;
        }
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            clean = clean.replace("\"", "\"\"");
            return "\"" + clean + "\"";
        }
        return clean;
    }

    public static byte[] generateParticipantsCsv(CampusEvent event, List<EventRegistration> registrations) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Header metadata
        pw.println("Event Title," + escapeCsv(event.getTitle()));
        pw.println("Event Category," + escapeCsv(event.getCategory()));
        pw.println("Venue," + escapeCsv(event.getVenue()));
        pw.println("Start Date & Time," + escapeCsv(event.getStartDatetime().format(DATE_FORMATTER)));
        pw.println("Total Capacity," + event.getCapacity());
        pw.println("Total Registered," + event.getRegisteredCount());
        pw.println();

        // Column Headers
        pw.println("Registration ID,Student Name,Email,Student ID Number,Department,Phone,Registered At,Status,Attendance Status,Checked-In At");

        for (EventRegistration reg : registrations) {
            String studentIdNo = reg.getStudent().getStudentIdNumber() != null ? reg.getStudent().getStudentIdNumber() : "N/A";
            String department = reg.getStudent().getDepartment() != null ? reg.getStudent().getDepartment() : "N/A";
            String phone = reg.getStudent().getPhone() != null ? reg.getStudent().getPhone() : "N/A";
            String registeredAt = reg.getRegisteredAt() != null ? reg.getRegisteredAt().format(DATE_FORMATTER) : "N/A";
            String attendanceStatus = (reg.getAttendance() != null && reg.getAttendance().getAttendanceStatus() != null)
                    ? reg.getAttendance().getAttendanceStatus().name() : "ABSENT";
            String checkedInAt = (reg.getAttendance() != null && reg.getAttendance().getCheckedInAt() != null)
                    ? reg.getAttendance().getCheckedInAt().format(DATE_FORMATTER) : "N/A";

            pw.println(String.join(",",
                    escapeCsv(reg.getRegistrationReference()),
                    escapeCsv(reg.getStudent().getFullName()),
                    escapeCsv(reg.getStudent().getEmail()),
                    escapeCsv(studentIdNo),
                    escapeCsv(department),
                    escapeCsv(phone),
                    escapeCsv(registeredAt),
                    escapeCsv(reg.getRegistrationStatus().name()),
                    escapeCsv(attendanceStatus),
                    escapeCsv(checkedInAt)
            ));
        }

        pw.flush();
        return sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static byte[] generateEventsSummaryCsv(List<CampusEvent> events) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Event ID,Title,Category,Venue,Start Datetime,Deadline,Capacity,Registered Count,Available Seats,Status");

        for (CampusEvent e : events) {
            pw.println(String.join(",",
                    String.valueOf(e.getId()),
                    escapeCsv(e.getTitle()),
                    escapeCsv(e.getCategory()),
                    escapeCsv(e.getVenue()),
                    escapeCsv(e.getStartDatetime().format(DATE_FORMATTER)),
                    escapeCsv(e.getRegistrationDeadline().format(DATE_FORMATTER)),
                    String.valueOf(e.getCapacity()),
                    String.valueOf(e.getRegisteredCount()),
                    String.valueOf(e.getAvailableSeats()),
                    escapeCsv(e.getStatus().name())
            ));
        }

        pw.flush();
        return sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

