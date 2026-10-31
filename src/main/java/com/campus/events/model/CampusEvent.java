package com.campus.events.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "events")
public class CampusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String category;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String venue;

    @NotNull
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @NotNull
    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private AppUser organizer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CampusEvent() {
    }

    public CampusEvent(String title, String description, String category, String venue,
                       LocalDateTime startDatetime, LocalDateTime endDatetime,
                       LocalDateTime registrationDeadline, Integer capacity, AppUser organizer) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.organizer = organizer;
        this.registeredCount = 0;
        this.status = EventStatus.OPEN;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.registeredCount == null) {
            this.registeredCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business helper methods
    public boolean isRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return status == EventStatus.OPEN 
                && now.isBefore(registrationDeadline) 
                && now.isBefore(startDatetime) 
                && registeredCount < capacity;
    }

    public int getAvailableSeats() {
        return Math.max(0, capacity - registeredCount);
    }

    public boolean isFull() {
        return registeredCount >= capacity;
    }

    public boolean isDeadlinePassed() {
        return LocalDateTime.now().isAfter(registrationDeadline);
    }

    public boolean isStarted() {
        return LocalDateTime.now().isAfter(startDatetime);
    }

    public double getOccupancyPercentage() {
        if (capacity == null || capacity == 0) return 0.0;
        return Math.min(100.0, (registeredCount * 100.0) / capacity);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(LocalDateTime startDatetime) {
        this.startDatetime = startDatetime;
    }

    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(LocalDateTime endDatetime) {
        this.endDatetime = endDatetime;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public AppUser getOrganizer() {
        return organizer;
    }

    public void setOrganizer(AppUser organizer) {
        this.organizer = organizer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CampusEvent that = (CampusEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

