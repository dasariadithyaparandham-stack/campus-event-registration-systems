package com.campus.events.service;

import com.campus.events.model.*;
import com.campus.events.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private ConflictDetectionService conflictDetectionService;

    private AppUser student;
    private CampusEvent eventA;
    private CampusEvent eventB;

    @BeforeEach
    void setUp() {
        student = new AppUser("John", "john@campus.edu", "pass", Role.STUDENT);
        student.setId(10L);

        AppUser admin = new AppUser("Admin", "admin@campus.edu", "pass", Role.ADMIN);
        admin.setId(1L);

        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 15, 10, 0);

        // Event A: 10:00 to 13:00
        eventA = new CampusEvent("Event A", "Desc", "TECH", "Auditorium",
                baseTime, baseTime.plusHours(3), baseTime.minusDays(1), 50, admin);
        eventA.setId(1L);

        // Event B: 11:30 to 14:00 (Overlaps with A!)
        eventB = new CampusEvent("Event B", "Desc", "WORKSHOP", "Lab 1",
                baseTime.plusMinutes(90), baseTime.plusHours(4), baseTime.minusDays(1), 40, admin);
        eventB.setId(2L);
    }

    @Test
    @DisplayName("Should detect overlapping time slots between two events")
    void testEventsOverlapTrue() {
        boolean overlaps = conflictDetectionService.doEventsOverlap(eventA, eventB);
        assertTrue(overlaps, "Events A and B should overlap between 11:30 and 13:00");
    }

    @Test
    @DisplayName("Should use 2-hour default fallback when end datetime is null")
    void testDefaultTwoHourFallback() {
        // Event C: 12:00 start, null end -> default 12:00 to 14:00
        CampusEvent eventC = new CampusEvent();
        eventC.setId(3L);
        eventC.setStartDatetime(LocalDateTime.of(2026, 11, 15, 12, 0));
        eventC.setEndDatetime(null); // Missing end time!

        // Overlaps with Event A (10:00 - 13:00)
        assertTrue(conflictDetectionService.doEventsOverlap(eventA, eventC));
    }

    @Test
    @DisplayName("Should return false when events take place at different times")
    void testEventsDoNotOverlap() {
        // Event D: 14:00 to 16:00 (Starts after Event A ends)
        CampusEvent eventD = new CampusEvent();
        eventD.setId(4L);
        eventD.setStartDatetime(LocalDateTime.of(2026, 11, 15, 14, 0));
        eventD.setEndDatetime(LocalDateTime.of(2026, 11, 15, 16, 0));

        assertFalse(conflictDetectionService.doEventsOverlap(eventA, eventD));
    }

    @Test
    @DisplayName("Should find conflicting event for registered student")
    void testFindConflictingEventForStudent() {
        EventRegistration regA = new EventRegistration();
        regA.setId(101L);
        regA.setEvent(eventA);
        regA.setStudent(student);
        regA.setRegistrationStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByStudentIdAndRegistrationStatusOrderByRegisteredAtDesc(10L, RegistrationStatus.CONFIRMED))
                .thenReturn(List.of(regA));

        Optional<CampusEvent> conflict = conflictDetectionService.findConflictingEvent(10L, eventB);

        assertTrue(conflict.isPresent());
        assertEquals("Event A", conflict.get().getTitle());
    }
}

