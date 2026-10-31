package com.campus.events.service;

import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.CertificateRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.util.PdfCertificateGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PdfCertificateGenerator pdfCertificateGenerator;

    public CertificateService(CertificateRepository certificateRepository,
                              RegistrationRepository registrationRepository,
                              AttendanceRepository attendanceRepository,
                              NotificationService notificationService,
                              AuditService auditService,
                              PdfCertificateGenerator pdfCertificateGenerator) {
        this.certificateRepository = certificateRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.pdfCertificateGenerator = pdfCertificateGenerator;
    }

    /**
     * Issues an official participation certificate for a confirmed attendee.
     * Enforces prerequisite: Participant must have verified attendance (PRESENT).
     */
    @Transactional
    public Certificate issueCertificate(Long registrationId, AppUser issuer) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with ID: " + registrationId));

        if (registration.getRegistrationStatus() != RegistrationStatus.CONFIRMED) {
            throw new RegistrationException("Certificates cannot be issued for cancelled registrations.");
        }

        // Validate verified attendance (PRESENT)
        Attendance attendance = attendanceRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new RegistrationException("Attendance record not found for this registration. Attendance must be marked PRESENT before a certificate can be issued."));

        if (attendance.getAttendanceStatus() != AttendanceStatus.PRESENT) {
            throw new RegistrationException("Certificate issue rejected: Participant attendance is marked as " + 
                    attendance.getAttendanceStatus().name() + ". Only participants marked PRESENT are eligible for certificates.");
        }

        // Return existing certificate if already issued
        Optional<Certificate> existing = certificateRepository.findByRegistrationId(registrationId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Generate unique verifiable certificate code: CERT-YYYYMMDD-XXXX
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String certCode = "CERT-" + datePart + "-" + randomSuffix;

        Certificate certificate = new Certificate(
                certCode,
                registration,
                registration.getStudent(),
                registration.getEvent(),
                registration.getStudent().getFullName(),
                registration.getEvent().getTitle(),
                registration.getEvent().getStartDatetime()
        );

        Certificate saved = certificateRepository.save(certificate);

        // Notify student
        notificationService.sendNotification(
                registration.getStudent(),
                "CERTIFICATE_ISSUED",
                "Certificate Available: " + registration.getEvent().getTitle(),
                "Congratulations! Your official certificate of participation for '" + 
                registration.getEvent().getTitle() + "' has been generated. Credential Code: " + certCode + "."
        );

        // Audit Log
        auditService.log(
                issuer != null ? issuer : registration.getStudent(),
                "CERTIFICATE_ISSUED",
                "Certificate",
                saved.getId(),
                "Certificate " + certCode + " issued to " + registration.getStudent().getEmail() + " for event " + registration.getEvent().getTitle()
        );

        return saved;
    }

    /**
     * Bulk issues certificates for all participants marked PRESENT in an event.
     */
    @Transactional
    public int issueCertificatesForEvent(Long eventId, AppUser adminUser) {
        List<EventRegistration> registrations = registrationRepository.findByEventIdAndRegistrationStatus(
                eventId, RegistrationStatus.CONFIRMED);

        int count = 0;
        for (EventRegistration reg : registrations) {
            Optional<Attendance> attOpt = attendanceRepository.findByRegistrationId(reg.getId());
            if (attOpt.isPresent() && attOpt.get().getAttendanceStatus() == AttendanceStatus.PRESENT) {
                if (!certificateRepository.existsByRegistrationId(reg.getId())) {
                    issueCertificate(reg.getId(), adminUser);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Public credential verification by certificate code.
     */
    @Transactional(readOnly = true)
    public Optional<Certificate> verifyCertificate(String certificateCode) {
        if (certificateCode == null || certificateCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return certificateRepository.findByCodeWithDetails(certificateCode.trim());
    }

    @Transactional(readOnly = true)
    public List<Certificate> getStudentCertificates(Long studentId) {
        return certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificateByRegistrationId(Long registrationId) {
        return certificateRepository.findByRegistrationId(registrationId);
    }

    public byte[] generatePdf(Certificate certificate) {
        return pdfCertificateGenerator.generateCertificate(certificate);
    }
}

