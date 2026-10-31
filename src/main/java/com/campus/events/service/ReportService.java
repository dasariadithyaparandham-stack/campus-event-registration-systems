package com.campus.events.service;

import com.campus.events.dto.DashboardStatsDto;
import com.campus.events.model.*;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import com.campus.events.repository.UserRepository;
import com.campus.events.util.CsvReportGenerator;
import com.campus.events.util.ExcelReportGenerator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;

    public ReportService(UserRepository userRepository,
                         EventRepository eventRepository,
                         RegistrationRepository registrationRepository,
                         AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getAdminDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalStudents(userRepository.countByRole(Role.STUDENT));
        stats.setTotalEvents(eventRepository.count());
        stats.setOpenEvents(eventRepository.countByStatus(EventStatus.OPEN));
        stats.setClosedEvents(eventRepository.countByStatus(EventStatus.CLOSED));
        stats.setCancelledEvents(eventRepository.countByStatus(EventStatus.CANCELLED));
        stats.setCompletedEvents(eventRepository.countByStatus(EventStatus.COMPLETED));

        stats.setTotalRegistrations(registrationRepository.count());
        stats.setConfirmedRegistrations(registrationRepository.countByRegistrationStatus(RegistrationStatus.CONFIRMED));

        long attendees = attendanceRepository.countByAttendanceStatus(AttendanceStatus.PRESENT);
        stats.setTotalAttendees(attendees);

        if (stats.getConfirmedRegistrations() > 0) {
            double pct = (attendees * 100.0) / stats.getConfirmedRegistrations();
            stats.setOverallAttendancePercentage(Math.round(pct * 10.0) / 10.0);
        } else {
            stats.setOverallAttendancePercentage(0.0);
        }

        stats.setPopularEvents(eventRepository.findTop5ByOrderByRegisteredCountDesc());
        stats.setRecentRegistrations(registrationRepository.findRecentRegistrations(PageRequest.of(0, 8)));

        return stats;
    }

    @Transactional(readOnly = true)
    public byte[] exportEventParticipantsCsv(Long eventId) {
        CampusEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        List<EventRegistration> registrations = registrationRepository.findParticipantsForReport(eventId, null);
        return CsvReportGenerator.generateParticipantsCsv(event, registrations);
    }

    @Transactional(readOnly = true)
    public byte[] exportEventParticipantsExcel(Long eventId) {
        CampusEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        List<EventRegistration> registrations = registrationRepository.findParticipantsForReport(eventId, null);
        return ExcelReportGenerator.generateParticipantsExcel(event, registrations);
    }

    @Transactional(readOnly = true)
    public byte[] exportAllEventsCsv() {
        List<CampusEvent> events = eventRepository.findAll();
        return CsvReportGenerator.generateEventsSummaryCsv(events);
    }

    @Transactional(readOnly = true)
    public byte[] exportAllEventsExcel() {
        List<CampusEvent> events = eventRepository.findAll();
        return ExcelReportGenerator.generateEventsSummaryExcel(events);
    }
}

