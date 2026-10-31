package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AttendanceService attendanceService;

    private AppUser admin;
    private AppUser student;
    private CampusEvent event;
    private EventRegistration registration;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        admin = new AppUser("Admin", "admin@campus.edu", "pass", Role.ADMIN);
        admin.setId(1L);

        student = new AppUser("Jane Student", "jane@campus.edu", "pass", Role.STUDENT);
        student.setId(3L);

        event = new CampusEvent();
        event.setId(20L);
        event.setTitle("Cybersecurity Summit");

        registration = new EventRegistration();
        registration.setId(100L);
        registration.setRegistrationReference("REG-CYBER-100");
        registration.setQrCodeToken("TKN-CYBER-TOKEN-XYZ");
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        registration.setStudent(student);
        registration.setEvent(event);

        attendance = new Attendance(registration);
        attendance.setId(500L);
        attendance.setAttendanceStatus(AttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("Should successfully check in participant by registration reference")
    void testCheckInByReferenceSuccess() {
        when(registrationRepository.findByRegistrationReference("REG-CYBER-100"))
                .thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(100L)).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkInParticipant("REG-CYBER-100", admin, "Verified ID");

        assertNotNull(result);
        assertEquals(AttendanceStatus.PRESENT, result.getAttendanceStatus());
        assertNotNull(result.getCheckedInAt());
        assertEquals(admin, result.getCheckedInBy());
        verify(notificationService, times(1)).sendNotification(eq(student), anyString(), anyString(), anyString());
        verify(auditService, times(1)).log(eq(admin), eq("ATTENDANCE_CHECK_IN"), eq("Attendance"), eq(500L), anyString());
    }

    @Test
    @DisplayName("Should successfully check in participant by QR token")
    void testCheckInByQrTokenSuccess() {
        when(registrationRepository.findByRegistrationReference("TKN-CYBER-TOKEN-XYZ")).thenReturn(Optional.empty());
        when(registrationRepository.findByQrCodeToken("TKN-CYBER-TOKEN-XYZ")).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(100L)).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkInParticipant("TKN-CYBER-TOKEN-XYZ", admin, "QR Scanner");

        assertNotNull(result);
        assertEquals(AttendanceStatus.PRESENT, result.getAttendanceStatus());
    }

    @Test
    @DisplayName("Should prevent duplicate check-in if student is already marked present")
    void testPreventDuplicateCheckIn() {
        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        when(registrationRepository.findByRegistrationReference("REG-CYBER-100")).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(100L)).thenReturn(Optional.of(attendance));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                attendanceService.checkInParticipant("REG-CYBER-100", admin, null));

        assertTrue(ex.getMessage().contains("already checked in"));
    }

    @Test
    @DisplayName("Should correctly calculate event attendance statistics")
    void testAttendanceStatsCalculation() {
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndRegistrationStatus(20L, RegistrationStatus.CONFIRMED)).thenReturn(50L);
        when(attendanceRepository.countByEventIdAndStatus(20L, AttendanceStatus.PRESENT)).thenReturn(40L);

        Map<String, Object> stats = attendanceService.getEventAttendanceStats(20L);

        assertEquals(50L, stats.get("totalConfirmed"));
        assertEquals(40L, stats.get("presentCount"));
        assertEquals(10L, stats.get("absentCount"));
        assertEquals(80.0, stats.get("attendancePercentage"));
    }
}

