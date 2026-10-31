package com.campus.events.model;

public enum RegistrationStatus {
    CONFIRMED("Confirmed", "badge-success"),
    CANCELLED("Cancelled", "badge-danger");

    private final String displayName;
    private final String badgeClass;

    RegistrationStatus(String displayName, String badgeClass) {
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

