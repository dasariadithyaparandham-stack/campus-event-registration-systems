package com.campus.events.service;

import com.campus.events.dto.EventFormDto;
import com.campus.events.exception.RegistrationException;
import com.campus.events.model.AppUser;
import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventStatus;
import com.campus.events.model.Role;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private EventService eventService;

    private AppUser adminUser;
    private EventFormDto validForm;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser("Admin User", "admin@campus.edu", "hashed", Role.ADMIN);
        adminUser.setId(1L);

        validForm = new EventFormDto();
        validForm.setTitle("AI Symposium 2026");
        validForm.setDescription("An in-depth symposium on machine learning architectures.");
        validForm.setCategory("TECHNICAL");
        validForm.setVenue("Auditorium A");
        validForm.setStartDatetime(LocalDateTime.now().plusDays(10));
        validForm.setEndDatetime(LocalDateTime.now().plusDays(10).plusHours(4));
        validForm.setRegistrationDeadline(LocalDateTime.now().plusDays(7));
        validForm.setCapacity(100);
        validForm.setStatus(EventStatus.OPEN);
    }

    @Test
    @DisplayName("Should successfully create an event when valid form is provided")
    void testCreateEventSuccess() {
        CampusEvent event = new CampusEvent();
        event.setId(10L);
        event.setTitle(validForm.getTitle());
        event.setCapacity(100);
        event.setRegisteredCount(0);

        when(eventRepository.save(any(CampusEvent.class))).thenReturn(event);

        CampusEvent created = eventService.createEvent(validForm, adminUser);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        verify(eventRepository, times(1)).save(any(CampusEvent.class));
        verify(auditService, times(1)).log(eq(adminUser), eq("EVENT_CREATED"), eq("CampusEvent"), eq(10L), anyString());
    }

    @Test
    @DisplayName("Should reject event creation when registration deadline is after start datetime")
    void testCreateEventInvalidDeadline() {
        validForm.setRegistrationDeadline(validForm.getStartDatetime().plusDays(1));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                eventService.createEvent(validForm, adminUser));

        assertTrue(ex.getMessage().contains("Registration deadline must be before"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject event update if new capacity is lower than registered count")
    void testUpdateEventCapacityTooLow() {
        CampusEvent existing = new CampusEvent();
        existing.setId(5L);
        existing.setCapacity(50);
        existing.setRegisteredCount(40);

        when(eventRepository.findById(5L)).thenReturn(Optional.of(existing));

        validForm.setCapacity(30); // 30 is less than 40 registered

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                eventService.updateEvent(5L, validForm, adminUser));

        assertTrue(ex.getMessage().contains("Capacity cannot be less than currently registered attendees"));
    }

    @Test
    @DisplayName("Should successfully transition status from DRAFT to OPEN")
    void testValidStatusTransition() {
        CampusEvent existing = new CampusEvent();
        existing.setId(1L);
        existing.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(CampusEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        CampusEvent updated = eventService.updateStatus(1L, EventStatus.OPEN, adminUser);

        assertEquals(EventStatus.OPEN, updated.getStatus());
        verify(auditService, times(1)).log(eq(adminUser), eq("EVENT_STATUS_CHANGED"), eq("CampusEvent"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Should reject status change on COMPLETED event")
    void testStatusChangeOnCompletedEvent() {
        CampusEvent existing = new CampusEvent();
        existing.setId(2L);
        existing.setStatus(EventStatus.COMPLETED);

        when(eventRepository.findById(2L)).thenReturn(Optional.of(existing));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                eventService.updateStatus(2L, EventStatus.OPEN, adminUser));

        assertTrue(ex.getMessage().contains("Cannot change status of a completed event"));
    }
}

