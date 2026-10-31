package com.campus.events.controller;

import com.campus.events.config.CustomUserDetails;
import com.campus.events.dto.CheckInRequest;
import com.campus.events.dto.CheckInResult;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.repository.EventRepository;
import com.campus.events.service.AttendanceService;
import com.campus.events.service.EventService;
import com.campus.events.service.FeedbackService;
import com.campus.events.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/organizer")
public class OrganizerController {

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final AttendanceService attendanceService;
    private final FeedbackService feedbackService;

    public OrganizerController(EventRepository eventRepository,
                               EventService eventService,
                               RegistrationService registrationService,
                               AttendanceService attendanceService,
                               FeedbackService feedbackService) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.attendanceService = attendanceService;
        this.feedbackService = feedbackService;
    }

    private CampusEvent validateOwnership(Long eventId, AppUser user) {
        CampusEvent event = eventService.getEventById(eventId);
        boolean isOwner = event.getOrganizer() != null && event.getOrganizer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("Access Denied: You are not authorized to manage event #" + eventId + ".");
        }
        return event;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser organizer = userDetails.getUser();
        List<CampusEvent> assignedEvents = eventRepository.findByOrganizerIdOrderByStartDatetimeDesc(organizer.getId());

        model.addAttribute("organizer", organizer);
        model.addAttribute("assignedEvents", assignedEvents);
        model.addAttribute("totalAssigned", assignedEvents.size());

        return "organizer/dashboard";
    }

    @GetMapping("/events/{id}/participants")
    public String listParticipants(@PathVariable Long id,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) RegistrationStatus status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "15") int size,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        CampusEvent event = validateOwnership(id, userDetails.getUser());

        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        Page<EventRegistration> participantsPage = registrationService.searchParticipants(id, keyword, status, pageable);

        model.addAttribute("event", event);
        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("participants", participantsPage.getContent());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");

        return "organizer/participants";
    }

    @GetMapping("/events/{id}/attendance")
    public String attendanceDesk(@PathVariable Long id,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        CampusEvent event = validateOwnership(id, userDetails.getUser());
        Map<String, Object> stats = attendanceService.getEventAttendanceStats(id);

        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").ascending());
        Page<EventRegistration> participantsPage = registrationService.searchParticipants(
                id, keyword, RegistrationStatus.CONFIRMED, pageable);

        model.addAttribute("event", event);
        model.addAttribute("stats", stats);
        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("participants", participantsPage.getContent());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("checkInRequest", new CheckInRequest());

        return "organizer/attendance";
    }

    @PostMapping("/events/{id}/attendance/check-in")
    public String checkIn(@PathVariable Long id,
                          @Valid @ModelAttribute("checkInRequest") CheckInRequest request,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        validateOwnership(id, userDetails.getUser());

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid identifier provided.");
            return "redirect:/organizer/events/" + id + "/attendance";
        }

        CheckInResult result = attendanceService.validateAndCheckIn(
                request.getIdentifier(), id, userDetails.getUser(), request.getNotes());

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

        return "redirect:/organizer/events/" + id + "/attendance";
    }

    @PostMapping("/events/{id}/attendance/toggle")
    public String toggleAttendance(@PathVariable Long id,
                                   @RequestParam Long registrationId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        validateOwnership(id, userDetails.getUser());

        try {
            Attendance attendance = attendanceService.toggleAttendance(registrationId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Updated attendance status for " + attendance.getRegistration().getStudent().getFullName() + 
                    " to " + attendance.getAttendanceStatus().name() + ".");
        } catch (RegistrationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/organizer/events/" + id + "/attendance";
    }

    @GetMapping("/events/{id}/feedback")
    public String viewFeedback(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        CampusEvent event = validateOwnership(id, userDetails.getUser());
        Map<String, Object> summary = feedbackService.getEventFeedbackSummary(id);

        model.addAttribute("event", event);
        model.addAttribute("feedbackSummary", summary);

        return "organizer/feedback";
    }
}

