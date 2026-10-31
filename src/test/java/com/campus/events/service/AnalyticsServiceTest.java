package com.campus.events.service;

import com.campus.events.dto.AnalyticsSummaryDto;
import com.campus.events.model.RegistrationStatus;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Should correctly calculate attendance, utilization, cancellation, and no-show percentages")
    void testCalculateAnalyticsFormulas() {
        // 100 total registrations, 80 confirmed, 20 cancelled, 60 present, 200 total capacity
        when(registrationRepository.countFilteredRegistrations(any(), any(), any())).thenReturn(100L);
        when(registrationRepository.countFilteredByStatus(RegistrationStatus.CONFIRMED, null, null, null)).thenReturn(80L);
        when(registrationRepository.countFilteredByStatus(RegistrationStatus.CANCELLED, null, null, null)).thenReturn(20L);
        when(registrationRepository.countFilteredPresentAttendance(any(), any(), any())).thenReturn(60L);
        when(eventRepository.sumCapacityByCategory(any())).thenReturn(200L);
        when(registrationRepository.countRegistrationsByCategory(any(), any())).thenReturn(Collections.emptyList());
        when(registrationRepository.countRegistrationsByDepartment(any(), any())).thenReturn(Collections.emptyList());
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsSummaryDto summary = analyticsService.calculateAnalytics(null, null, null);

        assertNotNull(summary);
        assertEquals(100L, summary.getTotalRegistrations());
        assertEquals(80L, summary.getTotalConfirmed());
        assertEquals(20L, summary.getTotalCancelled());
        assertEquals(60L, summary.getTotalPresent());

        // Attendance rate: 60 present / 80 confirmed = 75.0%
        assertEquals(75.0, summary.getAttendanceRate(), 0.1);

        // Capacity utilization: 100 registrations / 200 capacity = 50.0%
        assertEquals(50.0, summary.getCapacityUtilization(), 0.1);

        // Cancellation rate: 20 cancelled / 100 total = 20.0%
        assertEquals(20.0, summary.getCancellationRate(), 0.1);

        // No-show rate: (80 - 60) absent / 80 confirmed = 25.0%
        assertEquals(25.0, summary.getNoShowRate(), 0.1);
    }

    @Test
    @DisplayName("Should safely handle zero denominators without throwing division by zero exceptions")
    void testSafeHandlingOfZeroDenominators() {
        when(registrationRepository.countFilteredRegistrations(any(), any(), any())).thenReturn(0L);
        when(registrationRepository.countFilteredByStatus(RegistrationStatus.CONFIRMED, null, null, null)).thenReturn(0L);
        when(registrationRepository.countFilteredByStatus(RegistrationStatus.CANCELLED, null, null, null)).thenReturn(0L);
        when(registrationRepository.countFilteredPresentAttendance(any(), any(), any())).thenReturn(0L);
        when(eventRepository.sumCapacityByCategory(any())).thenReturn(0L);
        when(registrationRepository.countRegistrationsByCategory(any(), any())).thenReturn(Collections.emptyList());
        when(registrationRepository.countRegistrationsByDepartment(any(), any())).thenReturn(Collections.emptyList());
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsSummaryDto summary = analyticsService.calculateAnalytics(null, null, null);

        assertNotNull(summary);
        assertEquals(0.0, summary.getAttendanceRate());
        assertEquals(0.0, summary.getCapacityUtilization());
        assertEquals(0.0, summary.getCancellationRate());
        assertEquals(0.0, summary.getNoShowRate());
    }
}

