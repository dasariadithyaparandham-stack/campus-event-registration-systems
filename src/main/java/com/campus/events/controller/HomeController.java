package com.campus.events.controller;

import com.campus.events.config.CustomUserDetails;
import com.campus.events.model.*;
import com.campus.events.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class HomeController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ReportService reportService;
    private final CertificateService certificateService;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;
    private final ConflictDetectionService conflictDetectionService;

    public HomeController(EventService eventService,
                          RegistrationService registrationService,
                          ReportService reportService,
                          CertificateService certificateService,
                          WaitlistService waitlistService,
                          FeedbackService feedbackService,
                          ConflictDetectionService conflictDetectionService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.reportService = reportService;
        this.certificateService = certificateService;
        this.waitlistService = waitlistService;
        this.feedbackService = feedbackService;
        this.conflictDetectionService = conflictDetectionService;
    }

    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CampusEvent> upcomingEvents = eventService.getOpenUpcomingEvents();
        model.addAttribute("featuredEvents", upcomingEvents.stream().limit(6).toList());
        model.addAttribute("totalEvents", eventService.getAllEvents().size());
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);
        return "home";
    }

    @GetMapping("/events")
    public String listEvents(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String venue,
                             @RequestParam(required = false) EventStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "9") int size,
                             @RequestParam(defaultValue = "startDatetime") String sortBy,
                             @RequestParam(defaultValue = "asc") String direction,
                             Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
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
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);

        return "event-list";
    }

    @GetMapping("/events/{id}")
    public String eventDetails(@PathVariable Long id,
                               Model model,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        CampusEvent event = eventService.getEventById(id);
        model.addAttribute("event", event);

        boolean isRegistered = false;
        EventRegistration userRegistration = null;

        WaitlistEntry activeWaitlist = null;
        CampusEvent conflictingEvent = null;

        if (userDetails != null && userDetails.getUser() != null) {
            Long studentId = userDetails.getUser().getId();
            List<EventRegistration> registrations = registrationService.getStudentRegistrations(studentId);
            Optional<EventRegistration> activeReg = registrations.stream()
                    .filter(r -> r.getEvent().getId().equals(id) && r.getRegistrationStatus() == RegistrationStatus.CONFIRMED)
                    .findFirst();

            if (activeReg.isPresent()) {
                isRegistered = true;
                userRegistration = activeReg.get();
            } else {
                activeWaitlist = waitlistService.getActiveWaitlistForStudentAndEvent(studentId, id).orElse(null);
                conflictingEvent = conflictDetectionService.findConflictingEvent(studentId, event).orElse(null);
            }
        }

        Map<String, Object> feedbackSummary = feedbackService.getEventFeedbackSummary(id);

        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("userRegistration", userRegistration);
        model.addAttribute("activeWaitlist", activeWaitlist);
        model.addAttribute("conflictingEvent", conflictingEvent);
        model.addAttribute("feedbackSummary", feedbackSummary);
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);

        return "event-details";
    }

    @GetMapping("/verify-certificate")
    public String verifyCertificate(@RequestParam(required = false) String code,
                                    Model model,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("code", code != null ? code.trim() : "");
        model.addAttribute("searched", code != null && !code.trim().isEmpty());
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);

        if (code != null && !code.trim().isEmpty()) {
            Optional<Certificate> certOpt = certificateService.verifyCertificate(code.trim());
            model.addAttribute("certificate", certOpt.orElse(null));
            model.addAttribute("isValid", certOpt.isPresent());
        }

        return "verify-certificate";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "campus-event-system",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
}

