package com.campus.events.service;

import com.campus.events.dto.EventFormDto;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.model.AppUser;
import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventStatus;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AuditService auditService;

    public EventService(EventRepository eventRepository,
                        RegistrationRepository registrationRepository,
                        AuditService auditService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CampusEvent createEvent(EventFormDto dto, AppUser organizer) {
        validateEventForm(dto);

        CampusEvent event = new CampusEvent();
        copyDtoToEntity(dto, event);
        event.setOrganizer(organizer);
        event.setRegisteredCount(0);

        CampusEvent saved = eventRepository.save(event);
        auditService.log(organizer, "EVENT_CREATED", "CampusEvent", saved.getId(),
                "Event created: '" + saved.getTitle() + "' with capacity " + saved.getCapacity());
        return saved;
    }

    @Transactional
    public CampusEvent updateEvent(Long eventId, EventFormDto dto, AppUser modifier) {
        CampusEvent event = getEventById(eventId);
        validateEventForm(dto);

        // Cannot set capacity below already registered count
        if (dto.getCapacity() < event.getRegisteredCount()) {
            throw new RegistrationException("Capacity cannot be less than currently registered attendees (" + event.getRegisteredCount() + ")");
        }

        copyDtoToEntity(dto, event);
        CampusEvent updated = eventRepository.save(event);
        auditService.log(modifier, "EVENT_UPDATED", "CampusEvent", updated.getId(),
                "Event updated: '" + updated.getTitle() + "'");
        return updated;
    }

    @Transactional
    public CampusEvent updateStatus(Long eventId, EventStatus newStatus, AppUser modifier) {
        CampusEvent event = getEventById(eventId);
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == newStatus) {
            return event;
        }

        validateStatusTransition(currentStatus, newStatus);
        event.setStatus(newStatus);
        CampusEvent saved = eventRepository.save(event);

        auditService.log(modifier, "EVENT_STATUS_CHANGED", "CampusEvent", saved.getId(),
                "Status transitioned from " + currentStatus + " to " + newStatus);
        return saved;
    }

    @Transactional
    public void deleteEvent(Long eventId, AppUser modifier) {
        CampusEvent event = getEventById(eventId);
        if (event.getRegisteredCount() > 0) {
            throw new RegistrationException("Cannot delete event '" + event.getTitle() + "' because it has " + event.getRegisteredCount() + " registrations. Cancel or close the event instead.");
        }

        long regCount = registrationRepository.findByEventId(eventId).size();
        if (regCount > 0) {
            throw new RegistrationException("Cannot delete event with existing historical registration records.");
        }

        eventRepository.delete(event);
        auditService.log(modifier, "EVENT_DELETED", "CampusEvent", eventId, "Deleted event: " + event.getTitle());
    }

    @Transactional(readOnly = true)
    public CampusEvent getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    @Transactional(readOnly = true)
    public List<CampusEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CampusEvent> getOpenUpcomingEvents() {
        return eventRepository.findOpenUpcomingEvents(EventStatus.OPEN, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<CampusEvent> searchEvents(String keyword, String category, String venue, EventStatus status, Pageable pageable) {
        Specification<CampusEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchLike = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), searchLike);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchLike);
                Predicate venueLike = cb.like(cb.lower(root.get("venue")), searchLike);
                predicates.add(cb.or(titleLike, descLike, venueLike));
            }

            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }

            if (venue != null && !venue.trim().isEmpty() && !"ALL".equalsIgnoreCase(venue)) {
                predicates.add(cb.equal(root.get("venue"), venue.trim()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eventRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return eventRepository.findAllCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getAllVenues() {
        return eventRepository.findAllVenues();
    }

    private void validateEventForm(EventFormDto dto) {
        if (!dto.isDeadlineBeforeStart()) {
            throw new RegistrationException("Registration deadline must be before the event start time.");
        }
        if (!dto.isEndAfterStart()) {
            throw new RegistrationException("Event end time must be after the start time.");
        }
    }

    private void validateStatusTransition(EventStatus from, EventStatus to) {
        if (from == EventStatus.COMPLETED) {
            throw new RegistrationException("Cannot change status of a completed event.");
        }
        if (from == EventStatus.CANCELLED && to != EventStatus.OPEN && to != EventStatus.DRAFT) {
            throw new RegistrationException("A cancelled event can only be reopened or moved to draft.");
        }
    }

    private void copyDtoToEntity(EventFormDto dto, CampusEvent event) {
        event.setTitle(dto.getTitle().trim());
        event.setDescription(dto.getDescription().trim());
        event.setCategory(dto.getCategory().trim());
        event.setVenue(dto.getVenue().trim());
        event.setStartDatetime(dto.getStartDatetime());
        event.setEndDatetime(dto.getEndDatetime());
        event.setRegistrationDeadline(dto.getRegistrationDeadline());
        event.setCapacity(dto.getCapacity());
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }
    }
}

