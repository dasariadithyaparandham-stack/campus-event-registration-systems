package com.campus.events.controller;

import com.campus.events.config.CustomUserDetails;
import com.campus.events.dto.UserProfileDto;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.exception.UnauthorizedOperationException;
import com.campus.events.model.*;
import com.campus.events.service.*;
import com.campus.events.util.QrCodeGenerator;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final RegistrationService registrationService;
    private final EventService eventService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final WaitlistService waitlistService;
    private final CertificateService certificateService;
    private final FeedbackService feedbackService;

    public StudentController(RegistrationService registrationService,
                             EventService eventService,
                             UserService userService,
                             NotificationService notificationService,
                             WaitlistService waitlistService,
                             CertificateService certificateService,
                             FeedbackService feedbackService) {
        this.registrationService = registrationService;
        this.eventService = eventService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.waitlistService = waitlistService;
        this.certificateService = certificateService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser student = userDetails.getUser();
        List<EventRegistration> allRegistrations = registrationService.getStudentRegistrations(student.getId());

        LocalDateTime now = LocalDateTime.now();

        List<EventRegistration> upcomingActive = allRegistrations.stream()
                .filter(r -> r.isConfirmed() && r.getEvent().getStartDatetime().isAfter(now))
                .toList();

        List<EventRegistration> pastOrCompleted = allRegistrations.stream()
                .filter(r -> !r.getEvent().getStartDatetime().isAfter(now) || r.isCancelled())
                .toList();

        List<CampusEvent> recommendedEvents = eventService.getOpenUpcomingEvents().stream()
                .filter(e -> allRegistrations.stream().noneMatch(r -> r.getEvent().getId().equals(e.getId()) && r.isConfirmed()))
                .limit(4)
                .toList();

        List<Notification> notifications = notificationService.getUserNotifications(student.getId());
        List<WaitlistEntry> waitlistEntries = waitlistService.getStudentWaitlist(student.getId());
        List<Certificate> certificates = certificateService.getStudentCertificates(student.getId());
        long unreadNotificationsCount = notificationService.getUnreadCount(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("upcomingRegistrations", upcomingActive);
        model.addAttribute("pastRegistrations", pastOrCompleted);
        model.addAttribute("totalRegistered", allRegistrations.stream().filter(EventRegistration::isConfirmed).count());
        model.addAttribute("recommendedEvents", recommendedEvents);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadNotificationsCount);
        model.addAttribute("waitlistEntries", waitlistEntries);
        model.addAttribute("certificates", certificates);

        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser user = userService.getById(userDetails.getId());
        UserProfileDto dto = new UserProfileDto(
                user.getFullName(),
                user.getStudentIdNumber(),
                user.getPhone(),
                user.getDepartment()
        );
        model.addAttribute("profileDto", dto);
        model.addAttribute("user", user);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute("profileDto") UserProfileDto profileDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userDetails.getUser());
            return "student/profile";
        }

        userService.updateProfile(userDetails.getId(), profileDto);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/student/profile";
    }

    @PostMapping("/register/{eventId}")
    public String registerForEvent(@PathVariable Long eventId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            EventRegistration registration = registrationService.registerStudentForEvent(userDetails.getUser(), eventId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Your registration reference is " + registration.getRegistrationReference());
            return "redirect:/student/ticket/" + registration.getId();
        } catch (RegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + eventId;
        }
    }

    @PostMapping("/cancel/{registrationId}")
    public String cancelRegistration(@PathVariable Long registrationId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            registrationService.cancelRegistration(userDetails.getUser(), registrationId);
            redirectAttributes.addFlashAttribute("successMessage", "Registration has been cancelled successfully.");
        } catch (RegistrationException | UnauthorizedOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/dashboard";
    }

    @GetMapping("/ticket/{registrationId}")
    public String viewTicket(@PathVariable Long registrationId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {
        EventRegistration registration = registrationService.getRegistrationById(registrationId);

        // Security check: Only the owning student or an admin can access the ticket
        boolean isOwner = registration.getStudent().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You are not authorized to view this ticket.");
        }

        // Generate QR code base64 image data URI
        String qrDataUri = QrCodeGenerator.generateQrCodeDataUri(registration.getQrCodeToken(), 260, 260);

        model.addAttribute("registration", registration);
        model.addAttribute("event", registration.getEvent());
        model.addAttribute("student", registration.getStudent());
        model.addAttribute("attendance", registration.getAttendance());
        model.addAttribute("qrDataUri", qrDataUri);

        return "student/ticket";
    }

    @GetMapping("/ticket/{registrationId}/download")
    public ResponseEntity<byte[]> downloadTicketQr(@PathVariable Long registrationId,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        EventRegistration registration = registrationService.getRegistrationById(registrationId);

        boolean isOwner = registration.getStudent().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You are not authorized to download this ticket.");
        }

        byte[] qrBytes = QrCodeGenerator.generateQrCodeImageBytes(registration.getQrCodeToken(), 350, 350);
        String filename = "Ticket-" + registration.getRegistrationReference() + ".png";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrBytes);
    }

    // ==========================================
    // WAITLIST ACTIONS
    // ==========================================

    @PostMapping("/waitlist/{eventId}")
    public String joinWaitlist(@PathVariable Long eventId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            WaitlistEntry entry = waitlistService.joinWaitlist(userDetails.getUser(), eventId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Success! You have joined the waitlist for '" + entry.getEvent().getTitle() + 
                    "' at queue position #" + entry.getQueuePosition() + ".");
        } catch (RegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/waitlist/{id}/accept")
    public String acceptWaitlistOffer(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        try {
            EventRegistration reg = waitlistService.acceptOffer(userDetails.getUser(), id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Congratulations! You accepted the offer and registered for '" + reg.getEvent().getTitle() + 
                    "'. Reference: " + reg.getRegistrationReference());
            return "redirect:/student/ticket/" + reg.getId();
        } catch (RegistrationException | UnauthorizedOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    @PostMapping("/waitlist/{id}/decline")
    public String declineWaitlistOffer(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            waitlistService.declineOffer(userDetails.getUser(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Offer declined. The seat has been offered to the next student.");
        } catch (RegistrationException | UnauthorizedOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/dashboard";
    }

    @PostMapping("/waitlist/{id}/cancel")
    public String cancelWaitlistEntry(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        try {
            waitlistService.cancelWaitlist(userDetails.getUser(), id);
            redirectAttributes.addFlashAttribute("successMessage", "You have successfully left the waitlist.");
        } catch (RegistrationException | UnauthorizedOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/dashboard";
    }

    // ==========================================
    // CERTIFICATES
    // ==========================================

    @GetMapping("/certificates")
    public String viewCertificates(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Certificate> certificates = certificateService.getStudentCertificates(userDetails.getId());
        model.addAttribute("certificates", certificates);
        model.addAttribute("student", userDetails.getUser());
        return "student/certificates";
    }

    @PostMapping("/certificates/claim/{registrationId}")
    public String claimCertificate(@PathVariable Long registrationId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            Certificate cert = certificateService.issueCertificate(registrationId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Certificate generated successfully! Code: " + cert.getCertificateCode());
        } catch (RegistrationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/certificates";
    }

    @GetMapping("/certificates/{id}/download")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable Long id,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        Certificate cert = certificateService.getCertificateById(id);

        boolean isOwner = cert.getStudent().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You are not authorized to download this certificate.");
        }

        byte[] pdfBytes = certificateService.generatePdf(cert);
        String filename = "Certificate-" + cert.getCertificateCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ==========================================
    // FEEDBACK & RATINGS
    // ==========================================

    @GetMapping("/feedback/{registrationId}")
    public String feedbackForm(@PathVariable Long registrationId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        EventRegistration registration = registrationService.getRegistrationById(registrationId);

        if (!registration.getStudent().getId().equals(userDetails.getId())) {
            throw new UnauthorizedOperationException("You can only submit feedback for your own registrations.");
        }

        boolean alreadySubmitted = feedbackService.hasStudentProvidedFeedback(
                userDetails.getId(), registration.getEvent().getId());

        model.addAttribute("registration", registration);
        model.addAttribute("event", registration.getEvent());
        model.addAttribute("alreadySubmitted", alreadySubmitted);
        if (alreadySubmitted) {
            model.addAttribute("existingFeedback", feedbackService.getFeedbackByRegistrationId(registrationId).orElse(null));
        }

        return "student/feedback-form";
    }

    @PostMapping("/feedback/{registrationId}")
    public String submitFeedback(@PathVariable Long registrationId,
                                 @RequestParam Integer rating,
                                 @RequestParam(required = false) String comments,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            feedbackService.submitFeedback(userDetails.getUser(), registrationId, rating, comments);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your feedback has been recorded.");
            return "redirect:/student/dashboard";
        } catch (RegistrationException | UnauthorizedOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/feedback/" + registrationId;
        }
    }

    // ==========================================
    // NOTIFICATION CENTER
    // ==========================================

    @GetMapping("/notifications")
    public String notificationCenter(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Notification> notifications = notificationService.getUserNotifications(userDetails.getId());
        long unreadCount = notificationService.getUnreadCount(userDetails.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("student", userDetails.getUser());

        return "student/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(id);
        return "redirect:/student/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead(userDetails.getId());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/student/notifications";
    }
}

