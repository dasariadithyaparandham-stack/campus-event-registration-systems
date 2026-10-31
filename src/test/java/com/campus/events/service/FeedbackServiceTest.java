package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventFeedbackRepository;
import com.campus.events.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private EventFeedbackRepository feedbackRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FeedbackService feedbackService;

    private AppUser student;
    private CampusEvent event;
    private EventRegistration registration;

    @BeforeEach
    void setUp() {
        student = new AppUser("John Doe", "john.doe@campus.edu", "pass", Role.STUDENT);
        student.setId(10L);

        AppUser admin = new AppUser("Admin", "admin@campus.edu", "pass", Role.ADMIN);
        admin.setId(1L);

        event = new CampusEvent("Cybersecurity Workshop", "Desc", "WORKSHOP", "Lab 2",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(3),
                LocalDateTime.now().minusDays(3), 60, admin);
        event.setId(88L);

        registration = new EventRegistration();
        registration.setId(300L);
        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should reject feedback if rating is outside 1-5 stars")
    void testInvalidRatingRange() {
        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                feedbackService.submitFeedback(student, 300L, 6, "Excellent"));
        assertTrue(ex.getMessage().contains("between 1 and 5 stars"));
    }

    @Test
    @DisplayName("Should reject feedback if attendee was not marked PRESENT")
    void testFeedbackRejectedWhenAbsent() {
        Attendance absent = new Attendance(registration);
        absent.setAttendanceStatus(AttendanceStatus.ABSENT);

        when(registrationRepository.findById(300L)).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(300L)).thenReturn(Optional.of(absent));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                feedbackService.submitFeedback(student, 300L, 5, "Great"));

        assertTrue(ex.getMessage().contains("only allowed for verified attendees who were marked PRESENT"));
    }

    @Test
    @DisplayName("Should reject duplicate feedback for the same event")
    void testDuplicateFeedbackRejected() {
        Attendance present = new Attendance(registration);
        present.setAttendanceStatus(AttendanceStatus.PRESENT);

        when(registrationRepository.findById(300L)).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(300L)).thenReturn(Optional.of(present));
        when(feedbackRepository.existsByRegistrationId(300L)).thenReturn(true);

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                feedbackService.submitFeedback(student, 300L, 5, "Great"));

        assertTrue(ex.getMessage().contains("already submitted feedback"));
    }

    @Test
    @DisplayName("Should successfully record feedback and audit log")
    void testSubmitFeedbackSuccess() {
        Attendance present = new Attendance(registration);
        present.setAttendanceStatus(AttendanceStatus.PRESENT);

        when(registrationRepository.findById(300L)).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(300L)).thenReturn(Optional.of(present));
        when(feedbackRepository.existsByRegistrationId(300L)).thenReturn(false);
        when(feedbackRepository.existsByStudentIdAndEventId(10L, 88L)).thenReturn(false);
        when(feedbackRepository.save(any(EventFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        EventFeedback fb = feedbackService.submitFeedback(student, 300L, 5, "Extremely informative hands-on lab!");

        assertNotNull(fb);
        assertEquals(5, fb.getRating());
        assertEquals("Extremely informative hands-on lab!", fb.getComments());
        verify(feedbackRepository).save(any(EventFeedback.class));
        verify(auditService).log(eq(student), eq("FEEDBACK_SUBMITTED"), anyString(), any(), anyString());
    }
}

