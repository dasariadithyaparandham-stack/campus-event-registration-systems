package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventFeedbackRepository;
import com.campus.events.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeedbackService {

    private final EventFeedbackRepository feedbackRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditService auditService;

    public FeedbackService(EventFeedbackRepository feedbackRepository,
                           RegistrationRepository registrationRepository,
                           AttendanceRepository attendanceRepository,
                           AuditService auditService) {
        this.feedbackRepository = feedbackRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.auditService = auditService;
    }

    /**
     * Submits feedback and a 1-5 star rating for an attended event.
     */
    @Transactional
    public EventFeedback submitFeedback(AppUser student, Long registrationId, Integer rating, String comments) {
        if (student == null) {
            throw new UnauthorizedOperationException("Authentication required.");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new RegistrationException("Rating must be between 1 and 5 stars.");
        }

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with ID: " + registrationId));

        if (!registration.getStudent().getId().equals(student.getId())) {
            throw new UnauthorizedOperationException("You cannot submit feedback for another student's registration.");
        }

        // Must be marked PRESENT
        Attendance attendance = attendanceRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new RegistrationException("Attendance record not found."));

        if (attendance.getAttendanceStatus() != AttendanceStatus.PRESENT) {
            throw new RegistrationException("Feedback is only allowed for verified attendees who were marked PRESENT.");
        }

        // Prevent duplicate feedback
        if (feedbackRepository.existsByRegistrationId(registrationId) || 
            feedbackRepository.existsByStudentIdAndEventId(student.getId(), registration.getEvent().getId())) {
            throw new RegistrationException("You have already submitted feedback for this event.");
        }

        EventFeedback feedback = new EventFeedback(
                registration.getEvent(),
                student,
                registration,
                rating,
                comments != null ? comments.trim() : ""
        );

        EventFeedback saved = feedbackRepository.save(feedback);

        auditService.log(
                student,
                "FEEDBACK_SUBMITTED",
                "EventFeedback",
                saved.getId(),
                "Submitted " + rating + "-star rating for '" + registration.getEvent().getTitle() + "'"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<EventFeedback> getFeedbackForEvent(Long eventId) {
        return feedbackRepository.findByEventIdOrderByCreatedAtDesc(eventId);
    }

    @Transactional(readOnly = true)
    public Double getAverageRatingForEvent(Long eventId) {
        Double avg = feedbackRepository.getAverageRatingByEventId(eventId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Transactional(readOnly = true)
    public long getFeedbackCountForEvent(Long eventId) {
        return feedbackRepository.countByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public boolean hasStudentProvidedFeedback(Long studentId, Long eventId) {
        return feedbackRepository.existsByStudentIdAndEventId(studentId, eventId);
    }

    @Transactional(readOnly = true)
    public Optional<EventFeedback> getFeedbackByRegistrationId(Long registrationId) {
        return feedbackRepository.findByRegistrationId(registrationId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventFeedbackSummary(Long eventId) {
        List<EventFeedback> feedbacks = getFeedbackForEvent(eventId);
        double avg = getAverageRatingForEvent(eventId);
        long count = getFeedbackCountForEvent(eventId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("feedbacks", feedbacks);
        summary.put("averageRating", avg);
        summary.put("totalReviews", count);
        return summary;
    }
}

