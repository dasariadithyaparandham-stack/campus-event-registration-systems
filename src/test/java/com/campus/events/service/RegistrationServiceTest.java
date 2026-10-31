package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
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
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private ConflictDetectionService conflictDetectionService;

    @Mock
    private WaitlistService waitlistService;

    @InjectMocks
    private RegistrationService registrationService;

    private AppUser student;
    private CampusEvent openEvent;

    @BeforeEach
    void setUp() {
        student = new AppUser("John Doe", "john.doe@campus.edu", "hashed", Role.STUDENT);
        student.setId(2L);

        openEvent = new CampusEvent();
        openEvent.setId(100L);
        openEvent.setTitle("Robotics Hackathon 2026");
        openEvent.setStatus(EventStatus.OPEN);
        openEvent.setCapacity(50);
        openEvent.setRegisteredCount(10);
        openEvent.setStartDatetime(LocalDateTime.now().plusDays(5));
        openEvent.setRegistrationDeadline(LocalDateTime.now().plusDays(3));

        lenient().when(conflictDetectionService.findConflictingEvent(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Should successfully register student and increment capacity atomically")
    void testRegisterStudentSuccess() {
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(2L, 100L, RegistrationStatus.CONFIRMED))
                .thenReturn(false);
        when(registrationRepository.findByStudentIdAndEventId(2L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> {
            EventRegistration r = inv.getArgument(0);
            r.setId(999L);
            return r;
        });
        when(attendanceRepository.findByRegistrationId(anyLong())).thenReturn(Optional.empty());

        EventRegistration result = registrationService.registerStudentForEvent(student, 100L);

        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getRegistrationStatus());
        assertNotNull(result.getRegistrationReference());
        assertTrue(result.getRegistrationReference().startsWith("REG-"));
        assertEquals(11, openEvent.getRegisteredCount()); // Atomically incremented

        verify(eventRepository, times(1)).save(openEvent);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(notificationService, times(1)).sendNotification(eq(student), anyString(), anyString(), anyString());
        verify(auditService, times(1)).log(eq(student), eq("REGISTRATION_CREATED"), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should reject duplicate registration if student is already confirmed")
    void testDuplicateRegistrationRejected() {
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(2L, 100L, RegistrationStatus.CONFIRMED))
                .thenReturn(true);

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                registrationService.registerStudentForEvent(student, 100L));

        assertTrue(ex.getMessage().contains("You are already registered for this event"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when event reaches maximum capacity")
    void testCapacityEnforcement() {
        openEvent.setRegisteredCount(50); // Equal to capacity
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(2L, 100L, RegistrationStatus.CONFIRMED))
                .thenReturn(false);

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                registrationService.registerStudentForEvent(student, 100L));

        assertTrue(ex.getMessage().contains("maximum capacity"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration after registration deadline has elapsed")
    void testRegistrationDeadlinePassed() {
        openEvent.setRegistrationDeadline(LocalDateTime.now().minusHours(2));
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openEvent));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                registrationService.registerStudentForEvent(student, 100L));

        assertTrue(ex.getMessage().contains("Registration closed"));
    }

    @Test
    @DisplayName("Should successfully cancel registration and release seat capacity")
    void testCancelRegistrationSuccess() {
        EventRegistration reg = new EventRegistration();
        reg.setId(555L);
        reg.setStudent(student);
        reg.setEvent(openEvent);
        reg.setRegistrationReference("REG-20261029-TEST");
        reg.setRegistrationStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findById(555L)).thenReturn(Optional.of(reg));
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(openEvent));

        int initialCount = openEvent.getRegisteredCount();

        registrationService.cancelRegistration(student, 555L);

        assertEquals(RegistrationStatus.CANCELLED, reg.getRegistrationStatus());
        assertNotNull(reg.getCancelledAt());
        assertEquals(initialCount - 1, openEvent.getRegisteredCount()); // released seat

        verify(registrationRepository, times(1)).save(reg);
        verify(eventRepository, times(1)).save(openEvent);
    }

    @Test
    @DisplayName("Should reject cancellation by unauthorized other student")
    void testUnauthorizedCancellation() {
        AppUser otherStudent = new AppUser("Other Student", "other@campus.edu", "pass", Role.STUDENT);
        otherStudent.setId(99L);

        EventRegistration reg = new EventRegistration();
        reg.setId(555L);
        reg.setStudent(student); // Owned by student ID 2
        reg.setEvent(openEvent);
        reg.setRegistrationStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findById(555L)).thenReturn(Optional.of(reg));

        assertThrows(UnauthorizedOperationException.class, () ->
                registrationService.cancelRegistration(otherStudent, 555L));
    }
}

