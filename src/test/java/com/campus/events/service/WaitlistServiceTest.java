package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private WaitlistService waitlistService;

    private AppUser student;
    private CampusEvent fullEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waitlistService, "offerDurationHours", 24);

        student = new AppUser("John Doe", "john.doe@campus.edu", "pass", Role.STUDENT);
        student.setId(10L);

        AppUser organizer = new AppUser("Faculty", "prof@campus.edu", "pass", Role.ADMIN);
        organizer.setId(1L);

        fullEvent = new CampusEvent(
                "Full Tech Workshop",
                "Description",
                "TECHNICAL",
                "Auditorium",
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(5).plusHours(3),
                LocalDateTime.now().plusDays(3),
                50,
                organizer
        );
        fullEvent.setId(100L);
        fullEvent.setStatus(EventStatus.OPEN);
        fullEvent.setRegisteredCount(50); // Fully booked
    }

    @Test
    @DisplayName("Should successfully place student in waitlist queue when event is full")
    void testJoinWaitlistSuccess() {
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fullEvent));
        when(registrationRepository.existsByStudentIdAndEventIdAndRegistrationStatus(10L, 100L, RegistrationStatus.CONFIRMED))
                .thenReturn(false);
        when(waitlistRepository.existsByEventIdAndStudentIdAndStatusIn(eq(100L), eq(10L), any()))
                .thenReturn(false);
        when(waitlistRepository.findMaxQueuePositionByEventId(100L)).thenReturn(3);
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        WaitlistEntry entry = waitlistService.joinWaitlist(student, 100L);

        assertNotNull(entry);
        assertEquals(4, entry.getQueuePosition()); // Position incremented
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
        verify(notificationService).sendNotification(eq(student), eq("WAITLIST_JOINED"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject waitlist if seats are currently available")
    void testJoinWaitlistRejectedWhenSeatsAvailable() {
        fullEvent.setRegisteredCount(40); // 10 seats open!
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fullEvent));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                waitlistService.joinWaitlist(student, 100L));

        assertTrue(ex.getMessage().contains("Seats are currently available"));
        verify(waitlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate active waitlist entry")
    void testJoinWaitlistRejectedWhenAlreadyInQueue() {
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fullEvent));
        when(waitlistRepository.existsByEventIdAndStudentIdAndStatusIn(eq(100L), eq(10L), any()))
                .thenReturn(true);

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                waitlistService.joinWaitlist(student, 100L));

        assertTrue(ex.getMessage().contains("already on the active waitlist"));
    }

    @Test
    @DisplayName("Should promote next student and set 24h offer window when seat opens")
    void testPromoteNextStudent() {
        fullEvent.setRegisteredCount(49); // One seat opened!
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fullEvent));

        WaitlistEntry waitingStudent = new WaitlistEntry(fullEvent, student, 1);
        waitingStudent.setId(500L);
        when(waitlistRepository.findFirstByEventIdAndStatusOrderByQueuePositionAsc(100L, WaitlistStatus.WAITING))
                .thenReturn(Optional.of(waitingStudent));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<WaitlistEntry> result = waitlistService.promoteNextStudent(100L);

        assertTrue(result.isPresent());
        assertEquals(WaitlistStatus.OFFERED, result.get().getStatus());
        assertNotNull(result.get().getOfferedAt());
        assertNotNull(result.get().getOfferExpiresAt());
        assertEquals(50, fullEvent.getRegisteredCount()); // Reserved
        verify(notificationService).sendNotification(eq(student), eq("WAITLIST_OFFER"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should accept offer and convert into confirmed registration")
    void testAcceptOffer() {
        WaitlistEntry entry = new WaitlistEntry(fullEvent, student, 1);
        entry.setId(500L);
        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setOfferedAt(LocalDateTime.now().minusHours(2));
        entry.setOfferExpiresAt(LocalDateTime.now().plusHours(22));

        when(waitlistRepository.findById(500L)).thenReturn(Optional.of(entry));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRegistration reg = waitlistService.acceptOffer(student, 500L);

        assertNotNull(reg);
        assertEquals(RegistrationStatus.CONFIRMED, reg.getRegistrationStatus());
        assertEquals(WaitlistStatus.ACCEPTED, entry.getStatus());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Should decline offer, release seat, and promote next waiting student")
    void testDeclineOffer() {
        WaitlistEntry entry = new WaitlistEntry(fullEvent, student, 1);
        entry.setId(500L);
        entry.setStatus(WaitlistStatus.OFFERED);

        when(waitlistRepository.findById(500L)).thenReturn(Optional.of(entry));
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fullEvent));

        waitlistService.declineOffer(student, 500L);

        assertEquals(WaitlistStatus.DECLINED, entry.getStatus());
        assertEquals(49, fullEvent.getRegisteredCount()); // Released
        verify(eventRepository).save(fullEvent);
    }
}

