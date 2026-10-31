package com.campus.events.model;

public enum EventStatus {
    DRAFT("Draft", "badge-secondary"),
    OPEN("Open for Registration", "badge-success"),
    CLOSED("Registration Closed", "badge-warning"),
    CANCELLED("Cancelled", "badge-danger"),
    COMPLETED("Completed", "badge-info");

    private final String displayName;
    private final String badgeClass;

    EventStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}

