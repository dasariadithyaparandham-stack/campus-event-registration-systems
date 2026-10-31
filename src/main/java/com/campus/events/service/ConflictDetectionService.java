package com.campus.events.service;

import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventRegistration;
import com.campus.events.model.RegistrationStatus;
import com.campus.events.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConflictDetectionService {

    private final RegistrationRepository registrationRepository;

    public ConflictDetectionService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    /**
     * Determines whether two events overlap in time.
     * If an end time is missing, defaults to 2 hours after start time.
     */
    public boolean doEventsOverlap(CampusEvent a, CampusEvent b) {
        if (a == null || b == null || a.getId().equals(b.getId())) {
            return false;
        }

        LocalDateTime aStart = a.getStartDatetime();
        LocalDateTime aEnd = a.getEndDatetime() != null ? a.getEndDatetime() : aStart.plusHours(2);

        LocalDateTime bStart = b.getStartDatetime();
        LocalDateTime bEnd = b.getEndDatetime() != null ? b.getEndDatetime() : bStart.plusHours(2);

        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    /**
     * Checks if the student has any confirmed registration that conflicts with the specified event.
     */
    @Transactional(readOnly = true)
    public Optional<CampusEvent> findConflictingEvent(Long studentId, CampusEvent targetEvent) {
        List<CampusEvent> conflicts = findConflictingEvents(studentId, targetEvent);
        return conflicts.isEmpty() ? Optional.empty() : Optional.of(conflicts.get(0));
    }

    /**
     * Returns all confirmed events registered by this student that conflict with targetEvent.
     */
    @Transactional(readOnly = true)
    public List<CampusEvent> findConflictingEvents(Long studentId, CampusEvent targetEvent) {
        List<EventRegistration> activeRegistrations = registrationRepository
                .findByStudentIdAndRegistrationStatusOrderByRegisteredAtDesc(studentId, RegistrationStatus.CONFIRMED);

        List<CampusEvent> conflictingEvents = new ArrayList<>();
        for (EventRegistration reg : activeRegistrations) {
            CampusEvent existingEvent = reg.getEvent();
            if (doEventsOverlap(targetEvent, existingEvent)) {
                conflictingEvents.add(existingEvent);
            }
        }
        return conflictingEvents;
    }
}

