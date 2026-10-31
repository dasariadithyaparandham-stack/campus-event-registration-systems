package com.campus.events.service;

import com.campus.events.dto.DashboardStatsDto;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.repository.UserRepository;
import com.campus.events.util.CsvReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private ReportService reportService;

    private CampusEvent event;
    private AppUser student;
    private EventRegistration registration;

    @BeforeEach
    void setUp() {
        event = new CampusEvent();
        event.setId(1L);
        event.setTitle("Annual Hackathon");
        event.setCategory("HACKATHON");
        event.setVenue("Tech Lab");
        event.setCapacity(100);
        event.setRegisteredCount(10);
        event.setStartDatetime(LocalDateTime.now().plusDays(5));
        event.setRegistrationDeadline(LocalDateTime.now().plusDays(3));
        event.setStatus(EventStatus.OPEN);

        student = new AppUser("Alice Wonder", "alice@campus.edu", "pass", Role.STUDENT);
        student.setId(10L);

        registration = new EventRegistration();
        registration.setId(1L);
        registration.setRegistrationReference("REG-20261029-A1B2C3");
        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        registration.setRegisteredAt(LocalDateTime.now().minusDays(1));

        Attendance attendance = new Attendance(registration);
        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        attendance.setCheckedInAt(LocalDateTime.now().minusHours(2));
        registration.setAttendance(attendance);
    }

    @Test
    @DisplayName("Should escape potential CSV formula injection characters safely")
    void testCsvFormulaInjectionDefense() {
        assertEquals("Safe Text", CsvReportGenerator.escapeCsv("Safe Text"));
        assertEquals("'=1+1", CsvReportGenerator.escapeCsv("=1+1"));
        assertEquals("'+cmd|' /C calc'!A0", CsvReportGenerator.escapeCsv("+cmd|' /C calc'!A0"));
        assertEquals("'-2+5", CsvReportGenerator.escapeCsv("-2+5"));
        assertEquals("'@SUM(A1:A10)", CsvReportGenerator.escapeCsv("@SUM(A1:A10)"));
    }

    @Test
    @DisplayName("Should successfully generate participant CSV export")
    void testExportParticipantsCsv() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findParticipantsForReport(1L, null))
                .thenReturn(List.of(registration));

        byte[] csvBytes = reportService.exportEventParticipantsCsv(1L);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("Annual Hackathon"));
        assertTrue(csvContent.contains("REG-20261029-A1B2C3"));
        assertTrue(csvContent.contains("Alice Wonder"));
        assertTrue(csvContent.contains("alice@campus.edu"));
        assertTrue(csvContent.contains("PRESENT"));
    }

    @Test
    @DisplayName("Should successfully generate participant Excel (.xlsx) export")
    void testExportParticipantsExcel() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findParticipantsForReport(1L, null))
                .thenReturn(List.of(registration));

        byte[] excelBytes = reportService.exportEventParticipantsExcel(1L);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        // Valid ZIP/XLSX magic bytes: PK (0x50, 0x4B)
        assertEquals((byte) 0x50, excelBytes[0]);
        assertEquals((byte) 0x4B, excelBytes[1]);
    }

    @Test
    @DisplayName("Should compute admin dashboard statistics accurately from MySQL counts")
    void testGetAdminDashboardStats() {
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(150L);
        when(eventRepository.count()).thenReturn(10L);
        when(eventRepository.countByStatus(EventStatus.OPEN)).thenReturn(5L);
        when(eventRepository.countByStatus(EventStatus.CLOSED)).thenReturn(2L);
        when(eventRepository.countByStatus(EventStatus.CANCELLED)).thenReturn(1L);
        when(eventRepository.countByStatus(EventStatus.COMPLETED)).thenReturn(2L);
        when(registrationRepository.count()).thenReturn(200L);
        when(registrationRepository.countByRegistrationStatus(RegistrationStatus.CONFIRMED)).thenReturn(180L);
        when(attendanceRepository.countByAttendanceStatus(AttendanceStatus.PRESENT)).thenReturn(90L);

        DashboardStatsDto stats = reportService.getAdminDashboardStats();

        assertEquals(150L, stats.getTotalStudents());
        assertEquals(10L, stats.getTotalEvents());
        assertEquals(5L, stats.getOpenEvents());
        assertEquals(180L, stats.getConfirmedRegistrations());
        assertEquals(90L, stats.getTotalAttendees());
        assertEquals(50.0, stats.getOverallAttendancePercentage()); // 90 / 180 * 100 = 50.0%
    }
}

