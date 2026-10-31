package com.campus.events.controller;

import com.campus.events.config.CustomUserDetails;
import com.campus.events.dto.AnalyticsSummaryDto;
import com.campus.events.dto.CheckInRequest;
import com.campus.events.dto.CheckInResult;
import com.campus.events.dto.DashboardStatsDto;
import com.campus.events.dto.EventFormDto;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.model.*;
import com.campus.events.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;
    private final AuditService auditService;
    private final AnalyticsService analyticsService;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;
    private final CertificateService certificateService;

    public AdminController(EventService eventService,
                           RegistrationService registrationService,
                           AttendanceService attendanceService,
                           ReportService reportService,
                           AuditService auditService,
                           AnalyticsService analyticsService,
                           WaitlistService waitlistService,
                           FeedbackService feedbackService,
                           CertificateService certificateService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.attendanceService = attendanceService;
        this.reportService = reportService;
        this.auditService = auditService;
        this.analyticsService = analyticsService;
        this.waitlistService = waitlistService;
        this.feedbackService = feedbackService;
        this.certificateService = certificateService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDto stats = reportService.getAdminDashboardStats();
        List<AuditLog> recentLogs = auditService.getRecentLogs(6);

        model.addAttribute("stats", stats);
        model.addAttribute("recentLogs", recentLogs);
        return "admin/dashboard";
    }

    @GetMapping("/events")
    public String listEvents(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String venue,
                             @RequestParam(required = false) EventStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "id") String sortBy,
                             @RequestParam(defaultValue = "desc") String direction,
                             Model model) {

        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CampusEvent> eventPage = eventService.searchEvents(keyword, category, venue, status, pageable);

        model.addAttribute("eventPage", eventPage);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("venues", eventService.getAllVenues());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedCategory", category != null ? category : "");
        model.addAttribute("selectedVenue", venue != null ? venue : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/events";
    }

    @GetMapping("/events/new")
    public String showCreateEventForm(Model model) {
        if (!model.containsAttribute("eventForm")) {
            EventFormDto form = new EventFormDto();
            form.setStatus(EventStatus.DRAFT);
            model.addAttribute("eventForm", form);
        }
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("isEdit", false);
        return "admin/event-form";
    }

    @PostMapping("/events/new")
    public String createEvent(@Valid @ModelAttribute("eventForm") EventFormDto eventForm,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (!eventForm.isDeadlineBeforeStart()) {
            bindingResult.rejectValue("registrationDeadline", "error.eventForm", "Registration deadline must be before the event start time");
        }
        if (!eventForm.isEndAfterStart()) {
            bindingResult.rejectValue("endDatetime", "error.eventForm", "End time must be after the start time");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", eventService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/event-form";
        }

        try {
            CampusEvent created = eventService.createEvent(eventForm, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event '" + created.getTitle() + "' created successfully!");
            return "redirect:/admin/events";
        } catch (RegistrationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", eventService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/event-form";
        }
    }

    @GetMapping("/events/{id}/edit")
    public String showEditEventForm(@PathVariable Long id, Model model) {
        CampusEvent event = eventService.getEventById(id);

        EventFormDto form = new EventFormDto();
        form.setId(event.getId());
        form.setTitle(event.getTitle());
        form.setDescription(event.getDescription());
        form.setCategory(event.getCategory());
        form.setVenue(event.getVenue());
        form.setStartDatetime(event.getStartDatetime());
        form.setEndDatetime(event.getEndDatetime());
        form.setRegistrationDeadline(event.getRegistrationDeadline());
        form.setCapacity(event.getCapacity());
        form.setStatus(event.getStatus());

        model.addAttribute("eventForm", form);
        model.addAttribute("event", event);
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("isEdit", true);
        return "admin/event-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("eventForm") EventFormDto eventForm,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (!eventForm.isDeadlineBeforeStart()) {
            bindingResult.rejectValue("registrationDeadline", "error.eventForm", "Registration deadline must be before the event start time");
        }
        if (!eventForm.isEndAfterStart()) {
            bindingResult.rejectValue("endDatetime", "error.eventForm", "End time must be after the start time");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("event", eventService.getEventById(id));
            model.addAttribute("categories", eventService.getAllCategories());
            model.addAttribute("isEdit", true);
            return "admin/event-form";
        }

        try {
            CampusEvent updated = eventService.updateEvent(id, eventForm, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event '" + updated.getTitle() + "' updated successfully!");
            return "redirect:/admin/events";
        } catch (RegistrationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("event", eventService.getEventById(id));
            model.addAttribute("categories", eventService.getAllCategories());
            model.addAttribute("isEdit", true);
            return "admin/event-form";
        }
    }

    @PostMapping("/events/{id}/status")
    public String updateEventStatus(@PathVariable Long id,
                                    @RequestParam EventStatus status,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            CampusEvent event = eventService.updateStatus(id, status, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event '" + event.getTitle() + "' status changed to " + status.name() + ".");
        } catch (RegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        } catch (RegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/events";
    }

    @GetMapping("/participants")
    public String listParticipants(@RequestParam(required = false) Long eventId,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) RegistrationStatus status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "15") int size,
                                   Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        Page<EventRegistration> participantsPage = registrationService.searchParticipants(eventId, keyword, status, pageable);

        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("participants", participantsPage.getContent());
        model.addAttribute("events", eventService.getAllEvents());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");

        return "admin/participants";
    }

    @GetMapping("/attendance/{eventId}")
    public String attendanceDashboard(@PathVariable Long eventId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      Model model) {

        CampusEvent event = eventService.getEventById(eventId);
        Map<String, Object> stats = attendanceService.getEventAttendanceStats(eventId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").ascending());
        Page<EventRegistration> participantsPage = registrationService.searchParticipants(
                eventId, keyword, RegistrationStatus.CONFIRMED, pageable);

        model.addAttribute("event", event);
        model.addAttribute("stats", stats);
        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("participants", participantsPage.getContent());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("checkInRequest", new CheckInRequest());

        return "admin/attendance";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(@RequestParam Long eventId,
                          @Valid @ModelAttribute("checkInRequest") CheckInRequest request,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid identifier provided.");
            return "redirect:/admin/attendance/" + eventId;
        }

        CheckInResult result = attendanceService.validateAndCheckIn(
                request.getIdentifier(), eventId, userDetails.getUser(), request.getNotes());

        if (result.getStatus() == CheckInResult.Status.VALID) {
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
            redirectAttributes.addFlashAttribute("checkInResult", result);
        } else if (result.getStatus() == CheckInResult.Status.ALREADY_CHECKED_IN) {
            redirectAttributes.addFlashAttribute("warningMessage", result.getMessage());
            redirectAttributes.addFlashAttribute("checkInResult", result);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
            redirectAttributes.addFlashAttribute("checkInResult", result);
        }

        return "redirect:/admin/attendance/" + eventId;
    }

    @PostMapping("/attendance/toggle")
    public String toggleAttendance(@RequestParam Long eventId,
                                   @RequestParam Long registrationId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            Attendance attendance = attendanceService.toggleAttendance(registrationId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Updated attendance status for " + attendance.getRegistration().getStudent().getFullName() + 
                    " to " + attendance.getAttendanceStatus().name() + ".");
        } catch (RegistrationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/attendance/" + eventId;
    }

    @GetMapping("/reports")
    public String reportsCenter(Model model) {
        DashboardStatsDto stats = reportService.getAdminDashboardStats();
        List<CampusEvent> allEvents = eventService.getAllEvents();

        model.addAttribute("stats", stats);
        model.addAttribute("events", allEvents);
        return "admin/reports";
    }

    @GetMapping("/reports/participants/csv")
    public ResponseEntity<byte[]> exportParticipantsCsv(@RequestParam Long eventId) {
        CampusEvent event = eventService.getEventById(eventId);
        byte[] csvData = reportService.exportEventParticipantsCsv(eventId);
        String filename = "Participants-" + event.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/reports/participants/excel")
    public ResponseEntity<byte[]> exportParticipantsExcel(@RequestParam Long eventId) {
        CampusEvent event = eventService.getEventById(eventId);
        byte[] excelData = reportService.exportEventParticipantsExcel(eventId);
        String filename = "Participants-" + event.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/reports/events/csv")
    public ResponseEntity<byte[]> exportAllEventsCsv() {
        byte[] csvData = reportService.exportAllEventsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Campus_Events_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/reports/events/excel")
    public ResponseEntity<byte[]> exportAllEventsExcel() {
        byte[] excelData = reportService.exportAllEventsExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Campus_Events_Report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/logs")
    public String viewLogs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage = auditService.getAllLogs(pageable);

        model.addAttribute("logPage", logPage);
        model.addAttribute("logs", logPage.getContent());
        return "admin/logs";
    }

    // ==========================================
    // ENHANCEMENT C: ADVANCED ANALYTICS
    // ==========================================

    @GetMapping("/analytics")
    public String analyticsDashboard(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                     Model model) {

        AnalyticsSummaryDto summary = analyticsService.calculateAnalytics(category, fromDate, toDate);

        model.addAttribute("summary", summary);
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("selectedCategory", category != null ? category : "");
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "admin/analytics";
    }

    @GetMapping("/analytics/export/csv")
    public ResponseEntity<byte[]> exportAnalyticsCsv(@RequestParam(required = false) String category,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] csvData = analyticsService.exportAnalyticsCsv(category, fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Analytics_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/analytics/export/excel")
    public ResponseEntity<byte[]> exportAnalyticsExcel(@RequestParam(required = false) String category,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] excelData = analyticsService.exportAnalyticsExcel(category, fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Analytics_Report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    // ==========================================
    // WAITLIST MANAGEMENT
    // ==========================================

    @GetMapping("/waitlist/{eventId}")
    public String viewEventWaitlist(@PathVariable Long eventId, Model model) {
        CampusEvent event = eventService.getEventById(eventId);
        List<WaitlistEntry> waitlist = waitlistService.getEventWaitlist(eventId);

        model.addAttribute("event", event);
        model.addAttribute("waitlist", waitlist);
        return "admin/waitlist";
    }

    @PostMapping("/waitlist/expire")
    public String expireStaleWaitlistOffers(RedirectAttributes redirectAttributes) {
        int expired = waitlistService.processExpiredOffers();
        redirectAttributes.addFlashAttribute("successMessage", 
                "Expired " + expired + " stale waitlist offer(s) and auto-promoted next eligible students.");
        return "redirect:/admin/events";
    }

    // ==========================================
    // CERTIFICATES BULK ISSUANCE
    // ==========================================

    @PostMapping("/certificates/issue/{eventId}")
    public String bulkIssueCertificates(@PathVariable Long eventId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        RedirectAttributes redirectAttributes) {
        try {
            int count = certificateService.issueCertificatesForEvent(eventId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully generated " + count + " certificate(s) for verified attendees (PRESENT).");
        } catch (RegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/attendance/" + eventId;
    }

    // ==========================================
    // FEEDBACK & REVIEWS INSPECTION
    // ==========================================

    @GetMapping("/feedback/{eventId}")
    public String viewEventFeedback(@PathVariable Long eventId, Model model) {
        CampusEvent event = eventService.getEventById(eventId);
        Map<String, Object> summary = feedbackService.getEventFeedbackSummary(eventId);

        model.addAttribute("event", event);
        model.addAttribute("feedbackSummary", summary);
        return "admin/feedback";
    }
}
