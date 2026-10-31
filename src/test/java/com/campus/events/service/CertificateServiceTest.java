package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.CertificateRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.util.PdfCertificateGenerator;
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
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private PdfCertificateGenerator pdfCertificateGenerator;

    @InjectMocks
    private CertificateService certificateService;

    private AppUser student;
    private CampusEvent event;
    private EventRegistration registration;

    @BeforeEach
    void setUp() {
        student = new AppUser("John Doe", "john.doe@campus.edu", "pass", Role.STUDENT);
        student.setId(10L);
        student.setStudentIdNumber("STU-100");
        student.setDepartment("Computer Science");

        AppUser admin = new AppUser("Admin", "admin@campus.edu", "pass", Role.ADMIN);
        admin.setId(1L);

        event = new CampusEvent("AI Summit", "Desc", "TECH", "Hall A",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(4),
                LocalDateTime.now().minusDays(5), 100, admin);
        event.setId(50L);

        registration = new EventRegistration();
        registration.setId(200L);
        registration.setRegistrationReference("REG-20261029-ABCD");
        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationStatus(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should reject certificate generation if attendance is ABSENT")
    void testCertificateRejectedWhenAbsent() {
        Attendance absentAttendance = new Attendance(registration);
        absentAttendance.setAttendanceStatus(AttendanceStatus.ABSENT);

        when(registrationRepository.findById(200L)).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(200L)).thenReturn(Optional.of(absentAttendance));

        RegistrationException ex = assertThrows(RegistrationException.class, () ->
                certificateService.issueCertificate(200L, student));

        assertTrue(ex.getMessage().contains("Only participants marked PRESENT are eligible"));
        verify(certificateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully issue certificate when attendance is PRESENT")
    void testCertificateIssuedWhenPresent() {
        Attendance presentAttendance = new Attendance(registration);
        presentAttendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        presentAttendance.setCheckedInAt(LocalDateTime.now().minusDays(2));

        when(registrationRepository.findById(200L)).thenReturn(Optional.of(registration));
        when(attendanceRepository.findByRegistrationId(200L)).thenReturn(Optional.of(presentAttendance));
        when(certificateRepository.findByRegistrationId(200L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));

        Certificate cert = certificateService.issueCertificate(200L, student);

        assertNotNull(cert);
        assertNotNull(cert.getCertificateCode());
        assertTrue(cert.getCertificateCode().startsWith("CERT-"));
        assertEquals("John Doe", cert.getStudentName());
        assertEquals("AI Summit", cert.getEventTitle());
        assertEquals(CertificateStatus.ACTIVE, cert.getStatus());
        verify(certificateRepository).save(any(Certificate.class));
        verify(notificationService).sendNotification(eq(student), eq("CERTIFICATE_ISSUED"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should verify certificate code via public verifier")
    void testVerifyCertificateByCode() {
        Certificate existing = new Certificate(
                "CERT-20261029-A7B2", registration, student, event,
                "John Doe", "AI Summit", event.getStartDatetime());

        when(certificateRepository.findByCodeWithDetails("CERT-20261029-A7B2")).thenReturn(Optional.of(existing));

        Optional<Certificate> verified = certificateService.verifyCertificate("CERT-20261029-A7B2");

        assertTrue(verified.isPresent());
        assertEquals("John Doe", verified.get().getStudentName());
    }

    @Test
    @DisplayName("Should generate PDF certificate bytes with OpenPDF generator")
    void testGeneratePdfCertificate() {
        Certificate cert = new Certificate(
                "CERT-20261029-A7B2", registration, student, event,
                "John Doe", "AI Summit", event.getStartDatetime());

        byte[] fakePdf = new byte[]{1, 2, 3};
        when(pdfCertificateGenerator.generateCertificate(cert)).thenReturn(fakePdf);

        byte[] result = certificateService.generatePdf(cert);
        assertArrayEquals(fakePdf, result);
    }
}

