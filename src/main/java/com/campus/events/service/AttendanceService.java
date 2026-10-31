package com.campus.events.service;

import com.campus.events.dto.CheckInResult;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             RegistrationRepository registrationRepository,
                             EventRepository eventRepository,
                             NotificationService notificationService,
                             AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Comprehensive check-in evaluation returning a detailed CheckInResult
     * distinguishing VALID, ALREADY_CHECKED_IN, CANCELLED, WRONG_EVENT, and INVALID_TOKEN.
     */
    @Transactional
    public CheckInResult validateAndCheckIn(String identifier, Long deskEventId, AppUser staffUser, String notes) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return CheckInResult.invalidToken("(empty identifier)");
        }

        String trimmed = identifier.trim();
        Optional<EventRegistration> regOpt = registrationRepository.findByRegistrationReference(trimmed)
                .or(() -> registrationRepository.findByQrCodeToken(trimmed));

        if (regOpt.isEmpty()) {
            return CheckInResult.invalidToken(trimmed);
        }

        EventRegistration registration = regOpt.get();

        // Check if desk is locked to a specific event
        if (deskEventId != null && !registration.getEvent().getId().equals(deskEventId)) {
            CampusEvent deskEvent = eventRepository.findById(deskEventId).orElse(null);
            String deskTitle = deskEvent != null ? deskEvent.getTitle() : "Event #" + deskEventId;
            return CheckInResult.wrongEvent(
                    registration.getStudent().getFullName(),
                    registration.getRegistrationReference(),
                    registration.getEvent().getTitle(),
                    deskTitle
            );
        }

        // Check if registration was cancelled
        if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
            return CheckInResult.cancelled(
                    registration.getStudent().getFullName(),
                    registration.getRegistrationReference()
            );
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElse(new Attendance(registration));

        // Check if already checked in
        if (attendance.getAttendanceStatus() == AttendanceStatus.PRESENT) {
            String staff = attendance.getCheckedInBy() != null ? attendance.getCheckedInBy().getFullName() : "Admin";
            return CheckInResult.alreadyCheckedIn(
                    registration.getStudent().getFullName(),
                    registration.getRegistrationReference(),
                    attendance.getCheckedInAt(),
                    staff
            );
        }

        // Perform check-in
        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        attendance.setCheckedInAt(LocalDateTime.now());
        attendance.setCheckedInBy(staffUser);
        if (notes != null && !notes.trim().isEmpty()) {
            attendance.setNotes(notes.trim());
        } else {
            attendance.setNotes("Checked in by " + staffUser.getFullName());
        }

        Attendance saved = attendanceRepository.save(attendance);

        // Notify student of recorded attendance
        notificationService.sendNotification(
                registration.getStudent(),
                "ATTENDANCE_RECORDED",
                "Attendance Confirmed: " + registration.getEvent().getTitle(),
                "Your attendance was recorded for '" + registration.getEvent().getTitle() + "' at " + saved.getCheckedInAt()
        );

        // Audit log
        auditService.log(
                staffUser,
                "ATTENDANCE_CHECK_IN",
                "Attendance",
                saved.getId(),
                "Checked in student " + registration.getStudent().getEmail() + " for event " + registration.getEvent().getTitle()
        );

        return CheckInResult.valid(
                registration.getStudent().getFullName(),
                registration.getStudent().getEmail(),
                registration.getStudent().getStudentIdNumber(),
                registration.getEvent().getTitle(),
                registration.getRegistrationReference(),
                saved.getCheckedInAt(),
                staffUser.getFullName()
        );
    }

    /**
     * Checks in a participant using either their Registration Reference or QR Code Token.
     */
    @Transactional
    public Attendance checkInParticipant(String identifier, AppUser adminUser, String notes) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new RegistrationException("Ticket identifier is required.");
        }

        String trimmed = identifier.trim();
        EventRegistration registration = registrationRepository.findByRegistrationReference(trimmed)
                .or(() -> registrationRepository.findByQrCodeToken(trimmed))
                .orElseThrow(() -> new ResourceNotFoundException("No valid registration found matching ticket identifier '" + trimmed + "'."));

        if (registration.getRegistrationStatus() != RegistrationStatus.CONFIRMED) {
            throw new RegistrationException("Check-in rejected: Registration status is " + registration.getRegistrationStatus().name() + ".");
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElse(new Attendance(registration));

        if (attendance.getAttendanceStatus() == AttendanceStatus.PRESENT) {
            throw new RegistrationException("Participant '" + registration.getStudent().getFullName() + 
                    "' was already checked in at " + attendance.getCheckedInAt());
        }

        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        attendance.setCheckedInAt(LocalDateTime.now());
        attendance.setCheckedInBy(adminUser);
        if (notes != null && !notes.trim().isEmpty()) {
            attendance.setNotes(notes.trim());
        } else {
            attendance.setNotes("Checked in by admin " + adminUser.getFullName());
        }

        Attendance saved = attendanceRepository.save(attendance);

        // Notify student of recorded attendance
        notificationService.sendNotification(
                registration.getStudent(),
                "ATTENDANCE_RECORDED",
                "Attendance Confirmed: " + registration.getEvent().getTitle(),
                "Your attendance was recorded for '" + registration.getEvent().getTitle() + "' at " + saved.getCheckedInAt()
        );

        // Audit log
        auditService.log(
                adminUser,
                "ATTENDANCE_CHECK_IN",
                "Attendance",
                saved.getId(),
                "Checked in student " + registration.getStudent().getEmail() + " for event " + registration.getEvent().getTitle()
        );

        return saved;
    }

    /**
     * Toggles attendance between PRESENT and ABSENT (for authorized corrections).
     */
    @Transactional
    public Attendance toggleAttendance(Long registrationId, AppUser adminUser) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        if (registration.getRegistrationStatus() != RegistrationStatus.CONFIRMED) {
            throw new RegistrationException("Cannot change attendance for a cancelled registration.");
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElse(new Attendance(registration));

        if (attendance.getAttendanceStatus() == AttendanceStatus.PRESENT) {
            // Reverse attendance to ABSENT
            attendance.setAttendanceStatus(AttendanceStatus.ABSENT);
            attendance.setCheckedInAt(null);
            attendance.setCheckedInBy(adminUser);
            attendance.setNotes("Attendance reversed by " + adminUser.getFullName());
            auditService.log(adminUser, "ATTENDANCE_REVERSED", "Attendance", attendance.getId(),
                    "Reversed attendance for " + registration.getStudent().getEmail() + " to ABSENT");
        } else {
            // Mark PRESENT
            attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
            attendance.setCheckedInAt(LocalDateTime.now());
            attendance.setCheckedInBy(adminUser);
            attendance.setNotes("Marked present by " + adminUser.getFullName());
            auditService.log(adminUser, "ATTENDANCE_MARKED", "Attendance", attendance.getId(),
                    "Marked attendance for " + registration.getStudent().getEmail() + " as PRESENT");
        }

        return attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventAttendanceStats(Long eventId) {
        CampusEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        long totalRegistrations = registrationRepository.countByEventIdAndRegistrationStatus(eventId, RegistrationStatus.CONFIRMED);
        long presentCount = attendanceRepository.countByEventIdAndStatus(eventId, AttendanceStatus.PRESENT);
        long absentCount = Math.max(0, totalRegistrations - presentCount);
        double attendancePercentage = totalRegistrations > 0 ? (presentCount * 100.0) / totalRegistrations : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("event", event);
        stats.put("totalConfirmed", totalRegistrations);
        stats.put("presentCount", presentCount);
        stats.put("absentCount", absentCount);
        stats.put("attendancePercentage", Math.round(attendancePercentage * 10.0) / 10.0);
        return stats;
    }
}

