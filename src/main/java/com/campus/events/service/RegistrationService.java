package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ConflictDetectionService conflictDetectionService;
    private final WaitlistService waitlistService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               AttendanceRepository attendanceRepository,
                               NotificationService notificationService,
                               AuditService auditService,
                               ConflictDetectionService conflictDetectionService,
                               WaitlistService waitlistService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.conflictDetectionService = conflictDetectionService;
        this.waitlistService = waitlistService;
    }

    /**
     * Atomically registers a student for an event using pessimistic locking to prevent overbooking.
     */
    @Transactional
    public EventRegistration registerStudentForEvent(AppUser student, Long eventId) {
        if (student == null || student.getId() == null) {
            throw new UnauthorizedOperationException("Authentication required to register for events.");
        }

        // Concurrency protection: Lock the event row for update
        CampusEvent event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        LocalDateTime now = LocalDateTime.now();

        // 1. Validate Event Status
        if (event.getStatus() != EventStatus.OPEN) {
            throw new RegistrationException("Registrations are not accepted. Event status is " + event.getStatus().getDisplayName() + ".");
        }

        // 2. Validate Registration Deadline
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw new RegistrationException("Registration closed on " + 
                    event.getRegistrationDeadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) + ".");
        }

        // 3. Validate Event Start Time
        if (now.isAfter(event.getStartDatetime())) {
            throw new RegistrationException("Cannot register for an event that has already commenced.");
        }

        // 4. Validate Duplicate Registration
        boolean alreadyRegistered = registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(
                student.getId(), event.getId(), RegistrationStatus.CONFIRMED);
        if (alreadyRegistered) {
            throw new RegistrationException("You are already registered for this event.");
        }

        // 4b. Schedule Conflict Detection
        conflictDetectionService.findConflictingEvent(student.getId(), event).ifPresent(conflict -> {
            throw new RegistrationException("Schedule Conflict: You are already registered for '" + conflict.getTitle() + 
                    "' which takes place during the same time window.");
        });

        // 5. Validate Capacity (Atomic check under lock)
        if (event.getRegisteredCount() >= event.getCapacity()) {
            throw new RegistrationException("Registration failed: This event has reached its maximum capacity of " + event.getCapacity() + " participants.");
        }

        // 6. Generate Unique Registration Reference & Non-guessable QR Token
        String timestampPart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String registrationRef = "REG-" + timestampPart + "-" + randomHex;
        String qrToken = "TKN-" + UUID.randomUUID().toString();

        // Check if there was a previous cancelled registration, reuse or create new
        EventRegistration registration = registrationRepository.findByStudentIdAndEventId(student.getId(), event.getId())
                .orElse(new EventRegistration());

        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationReference(registrationRef);
        registration.setQrCodeToken(qrToken);
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        registration.setRegisteredAt(now);
        registration.setCancelledAt(null);

        EventRegistration savedRegistration = registrationRepository.save(registration);

        // 7. Increment Event Registered Count Atomically
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        // 8. Initialize Attendance record (marked ABSENT until checked in)
        Attendance attendance = attendanceRepository.findByRegistrationId(savedRegistration.getId())
                .orElse(new Attendance(savedRegistration));
        attendance.setAttendanceStatus(AttendanceStatus.ABSENT);
        attendance.setCheckedInAt(null);
        attendance.setCheckedInBy(null);
        attendance.setNotes("Awaiting event check-in");
        attendanceRepository.save(attendance);

        // 9. Send Confirmation Notification
        notificationService.sendNotification(
                student,
                "REGISTRATION_CONFIRMED",
                "Registration Confirmed: " + event.getTitle(),
                "Your registration for '" + event.getTitle() + "' is confirmed! Reference ID: " + registrationRef + ". You can view or print your QR ticket from your dashboard."
        );

        // 10. Audit Log
        auditService.log(
                student,
                "REGISTRATION_CREATED",
                "EventRegistration",
                savedRegistration.getId(),
                "Registered for '" + event.getTitle() + "' (Ref: " + registrationRef + ")"
        );

        return savedRegistration;
    }

    /**
     * Cancels an eligible student registration and releases capacity atomically.
     */
    @Transactional
    public void cancelRegistration(AppUser student, Long registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration record not found."));

        // Validate ownership or admin role
        boolean isOwner = registration.getStudent().getId().equals(student.getId());
        boolean isAdmin = student.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You do not have permission to cancel this registration.");
        }

        if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
            throw new RegistrationException("This registration is already cancelled.");
        }

        // Lock event to release seat
        CampusEvent event = eventRepository.findByIdForUpdate(registration.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated event not found."));

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw new RegistrationException("Cancellations are not permitted after the registration deadline has passed.");
        }

        if (now.isAfter(event.getStartDatetime())) {
            throw new RegistrationException("Cannot cancel registration for an event that has already started.");
        }

        // Update registration status
        registration.setRegistrationStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(now);
        registrationRepository.save(registration);

        // Safely decrement event registered count
        event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
        eventRepository.save(event);

        // Auto-promote next student from waitlist if any
        waitlistService.promoteNextStudent(event.getId());

        // Notify student
        notificationService.sendNotification(
                registration.getStudent(),
                "REGISTRATION_CANCELLED",
                "Registration Cancelled: " + event.getTitle(),
                "Your registration (" + registration.getRegistrationReference() + ") for '" + event.getTitle() + "' has been successfully cancelled."
        );

        // Audit Log
        auditService.log(
                student,
                "REGISTRATION_CANCELLED",
                "EventRegistration",
                registration.getId(),
                "Cancelled registration " + registration.getRegistrationReference() + " for event: " + event.getTitle()
        );
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> getStudentRegistrations(Long studentId) {
        return registrationRepository.findByStudentIdOrderByRegisteredAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> getStudentActiveRegistrations(Long studentId) {
        return registrationRepository.findByStudentIdAndRegistrationStatusOrderByRegisteredAtDesc(
                studentId, RegistrationStatus.CONFIRMED);
    }

    @Transactional(readOnly = true)
    public EventRegistration getRegistrationById(Long registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));
    }

    @Transactional(readOnly = true)
    public EventRegistration getRegistrationByReference(String reference) {
        return registrationRepository.findByRegistrationReference(reference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No registration found with reference: " + reference));
    }

    @Transactional(readOnly = true)
    public EventRegistration getRegistrationByToken(String token) {
        return registrationRepository.findByQrCodeToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No registration found with ticket token: " + token));
    }

    @Transactional(readOnly = true)
    public Page<EventRegistration> searchParticipants(Long eventId, String keyword, RegistrationStatus status, Pageable pageable) {
        return registrationRepository.searchParticipants(eventId, keyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> getParticipantsForReport(Long eventId, RegistrationStatus status) {
        return registrationRepository.findParticipantsForReport(eventId, status);
    }
}

