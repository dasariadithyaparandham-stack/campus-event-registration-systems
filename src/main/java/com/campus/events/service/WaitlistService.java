package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.waitlist.offer-duration-hours:24}")
    private int offerDurationHours;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           EventRepository eventRepository,
                           RegistrationRepository registrationRepository,
                           AttendanceRepository attendanceRepository,
                           NotificationService notificationService,
                           AuditService auditService) {
        this.waitlistRepository = waitlistRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Add student to the event waitlist when the event is at full capacity.
     */
    @Transactional
    public WaitlistEntry joinWaitlist(AppUser student, Long eventId) {
        if (student == null) {
            throw new RegistrationException("User must be logged in to join the waitlist.");
        }

        // Lock event to ensure consistent capacity evaluation
        CampusEvent event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getStatus() != EventStatus.OPEN) {
            throw new RegistrationException("Cannot join waitlist: Event is " + event.getStatus().name() + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw new RegistrationException("Registration deadline for this event has passed.");
        }

        if (now.isAfter(event.getStartDatetime())) {
            throw new RegistrationException("Cannot join waitlist for an event that has already started.");
        }

        // Check if student already has a confirmed registration
        boolean alreadyRegistered = registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(
                student.getId(), eventId, RegistrationStatus.CONFIRMED);
        if (alreadyRegistered) {
            throw new RegistrationException("You are already registered for this event.");
        }

        // Check if already in active waitlist (WAITING or OFFERED)
        boolean alreadyWaitlisted = waitlistRepository.existsByEventIdAndStudentIdAndStatusIn(
                eventId, student.getId(), List.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED));
        if (alreadyWaitlisted) {
            throw new RegistrationException("You are already on the active waitlist for this event.");
        }

        // If seats are currently open, prompt student to register directly
        if (event.getRegisteredCount() < event.getCapacity()) {
            throw new RegistrationException("Seats are currently available for this event! You may register directly instead of joining the waitlist.");
        }

        int nextPosition = waitlistRepository.findMaxQueuePositionByEventId(eventId) + 1;
        WaitlistEntry entry = new WaitlistEntry(event, student, nextPosition);
        WaitlistEntry saved = waitlistRepository.save(entry);

        notificationService.sendNotification(
                student,
                "WAITLIST_JOINED",
                "Joined Waitlist: " + event.getTitle(),
                "You have been placed on the waitlist for '" + event.getTitle() + "'. Your queue position is #" + nextPosition + "."
        );

        auditService.log(
                student,
                "WAITLIST_JOINED",
                "WaitlistEntry",
                saved.getId(),
                "Student " + student.getEmail() + " joined waitlist for '" + event.getTitle() + "' at position #" + nextPosition
        );

        return saved;
    }

    /**
     * Promotes the next student in queue when a seat opens up.
     */
    @Transactional
    public Optional<WaitlistEntry> promoteNextStudent(Long eventId) {
        CampusEvent event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getRegisteredCount() >= event.getCapacity()) {
            return Optional.empty(); // No seats available
        }

        Optional<WaitlistEntry> nextWaiting = waitlistRepository.findFirstByEventIdAndStatusOrderByQueuePositionAsc(
                eventId, WaitlistStatus.WAITING);

        if (nextWaiting.isEmpty()) {
            return Optional.empty();
        }

        WaitlistEntry entry = nextWaiting.get();
        LocalDateTime now = LocalDateTime.now();

        // Reserve the seat for this candidate
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setOfferedAt(now);
        entry.setOfferExpiresAt(now.plusHours(offerDurationHours));
        WaitlistEntry updated = waitlistRepository.save(entry);

        notificationService.sendNotification(
                entry.getStudent(),
                "WAITLIST_OFFER",
                "Seat Available: " + event.getTitle(),
                "Good news! A seat has opened up for '" + event.getTitle() + "'. You have " + offerDurationHours + 
                " hours to claim your seat in your student dashboard before the offer expires."
        );

        auditService.log(
                entry.getStudent(),
                "WAITLIST_OFFERED",
                "WaitlistEntry",
                updated.getId(),
                "Seat offered to student " + entry.getStudent().getEmail() + " for event '" + event.getTitle() + "'"
        );

        return Optional.of(updated);
    }

    /**
     * Accepts a waitlist offer and turns it into a confirmed registration.
     */
    @Transactional
    public EventRegistration acceptOffer(AppUser student, Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist record not found."));

        if (!entry.getStudent().getId().equals(student.getId())) {
            throw new UnauthorizedOperationException("You cannot accept an offer intended for another student.");
        }

        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new RegistrationException("This waitlist entry is not in an OFFERED state (current status: " + entry.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        if (entry.getOfferExpiresAt() != null && now.isAfter(entry.getOfferExpiresAt())) {
            // Offer has expired
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            // Release seat and promote next
            CampusEvent event = eventRepository.findByIdForUpdate(entry.getEvent().getId()).orElse(null);
            if (event != null) {
                event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
                eventRepository.save(event);
                promoteNextStudent(event.getId());
            }
            throw new RegistrationException("Your offer has expired. The seat has been offered to the next student in the queue.");
        }

        CampusEvent event = entry.getEvent();

        // Create confirmed registration
        String registrationRef = "REG-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String qrToken = UUID.randomUUID().toString().replace("-", "");

        EventRegistration registration = new EventRegistration();
        registration.setRegistrationReference(registrationRef);
        registration.setQrCodeToken(qrToken);
        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        registration.setRegisteredAt(now);

        EventRegistration savedRegistration = registrationRepository.save(registration);

        Attendance attendance = new Attendance(savedRegistration);
        attendanceRepository.save(attendance);

        entry.setStatus(WaitlistStatus.ACCEPTED);
        entry.setRespondedAt(now);
        waitlistRepository.save(entry);

        notificationService.sendNotification(
                student,
                "REGISTRATION_CONFIRMED",
                "Waitlist Claim Confirmed: " + event.getTitle(),
                "Your seat for '" + event.getTitle() + "' is confirmed! Reference ID: " + registrationRef + "."
        );

        auditService.log(
                student,
                "WAITLIST_ACCEPTED",
                "WaitlistEntry",
                entry.getId(),
                "Student " + student.getEmail() + " claimed offered seat for '" + event.getTitle() + "' -> " + registrationRef
        );

        return savedRegistration;
    }

    /**
     * Declines a waitlist offer, releasing the reserved seat and promoting the next student.
     */
    @Transactional
    public void declineOffer(AppUser student, Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist record not found."));

        boolean isOwner = entry.getStudent().getId().equals(student.getId());
        boolean isAdmin = student.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You do not have permission to decline this offer.");
        }

        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new RegistrationException("This offer is no longer pending.");
        }

        CampusEvent event = eventRepository.findByIdForUpdate(entry.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        entry.setStatus(WaitlistStatus.DECLINED);
        entry.setRespondedAt(LocalDateTime.now());
        waitlistRepository.save(entry);

        // Release reserved seat
        event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
        eventRepository.save(event);

        auditService.log(
                student,
                "WAITLIST_DECLINED",
                "WaitlistEntry",
                entry.getId(),
                "Student " + entry.getStudent().getEmail() + " declined waitlist offer for '" + event.getTitle() + "'"
        );

        // Offer seat to next student in line
        promoteNextStudent(event.getId());
    }

    /**
     * Cancels a student's waitlist queue entry.
     */
    @Transactional
    public void cancelWaitlist(AppUser student, Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist record not found."));

        boolean isOwner = entry.getStudent().getId().equals(student.getId());
        boolean isAdmin = student.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You do not have permission to cancel this waitlist entry.");
        }

        if (entry.getStatus() == WaitlistStatus.OFFERED) {
            declineOffer(student, waitlistId);
            return;
        }

        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new RegistrationException("Cannot cancel waitlist entry with status " + entry.getStatus() + ".");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        entry.setRespondedAt(LocalDateTime.now());
        waitlistRepository.save(entry);

        auditService.log(
                student,
                "WAITLIST_CANCELLED",
                "WaitlistEntry",
                entry.getId(),
                "Student " + entry.getStudent().getEmail() + " left waitlist for '" + entry.getEvent().getTitle() + "'"
        );
    }

    /**
     * Finds and expires stale offers that passed their deadline, releasing seats to subsequent students.
     */
    @Transactional
    public int processExpiredOffers() {
        List<WaitlistEntry> expired = waitlistRepository.findByStatusAndOfferExpiresAtBefore(
                WaitlistStatus.OFFERED, LocalDateTime.now());

        int count = 0;
        for (WaitlistEntry entry : expired) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            entry.setRespondedAt(LocalDateTime.now());
            waitlistRepository.save(entry);

            CampusEvent event = eventRepository.findByIdForUpdate(entry.getEvent().getId()).orElse(null);
            if (event != null) {
                event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
                eventRepository.save(event);
                promoteNextStudent(event.getId());
            }

            notificationService.sendNotification(
                    entry.getStudent(),
                    "WAITLIST_EXPIRED",
                    "Offer Expired: " + entry.getEvent().getTitle(),
                    "Your claim window for '" + entry.getEvent().getTitle() + "' has expired and the seat has been passed to the next student."
            );

            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> getStudentWaitlist(Long studentId) {
        return waitlistRepository.findByStudentIdOrderByJoinedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> getEventWaitlist(Long eventId) {
        return waitlistRepository.findByEventIdOrderByQueuePositionAsc(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<WaitlistEntry> getActiveWaitlistForStudentAndEvent(Long studentId, Long eventId) {
        return waitlistRepository.findByEventIdAndStudentIdAndStatusIn(
                eventId, studentId, List.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED));
    }
}

